package vn.macstudio;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import vn.macstudio.macstudio.repository.ProductRepository;
import vn.macstudio.macstudio.repository.AppUserRepository;
import vn.macstudio.macstudio.repository.CustomerOrderRepository;
import vn.macstudio.macstudio.repository.UserNotificationRepository;
import vn.macstudio.macstudio.model.AppUser;
import vn.macstudio.macstudio.model.OrderStatus;
import vn.macstudio.macstudio.service.OrderService;
import vn.macstudio.macstudio.service.CartService;
import vn.macstudio.macstudio.web.form.CartLine;
import vn.macstudio.macstudio.web.form.CheckoutForm;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MacStudioApplicationTests {
	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ProductRepository products;

	@Autowired
	private AppUserRepository users;

	@Autowired
	private CustomerOrderRepository orders;

	@Autowired
	private UserNotificationRepository notifications;

	@Autowired
	private OrderService orderService;

	@Autowired
	private CartService cart;

	private String testProductName;

	@BeforeEach
	void setUp() {
		testProductName = "Áo test " + System.nanoTime();
	}

	@Test
	void contextLoads() {
	}

	@Test
	void checkoutShowsCurrentProvinceAndWardFields() throws Exception {
		AppUser customer = users.findByEmailIgnoreCase("khach1@example.com").orElseThrow();
		var product = products.findAll().getFirst();
		MockHttpSession session = new MockHttpSession();
		cart.add(session, product.getId(), 1);

		mockMvc.perform(get("/cart/checkout").session(session).with(user(customer)))
			.andExpect(status().isOk())
			.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
				.string(containsString("Tỉnh / Thành phố")))
			.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
				.string(containsString("Phường / Xã")))
			.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
				.string(containsString("Địa chỉ cụ thể")))
			.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
				.string(containsString("/js/checkout.js")));
	}

	@Test
	void checkoutStoresTheComposedShippingAddress() throws Exception {
		AppUser customer = users.findByEmailIgnoreCase("khach1@example.com").orElseThrow();
		var product = products.findAll().getFirst();
		MockHttpSession session = new MockHttpSession();
		cart.add(session, product.getId(), 1);

		mockMvc.perform(post("/cart/checkout")
				.session(session)
				.with(user(customer))
				.with(csrf())
				.param("recipientName", customer.getFullName())
				.param("phone", "0900000000")
				.param("province", "Thành phố Hà Nội")
				.param("ward", "Phường Ba Đình")
				.param("specificAddress", "12 Nguyễn Huệ")
				.param("paymentMethod", "COD"))
			.andExpect(status().is3xxRedirection());

		org.junit.jupiter.api.Assertions.assertTrue(orders.findAll().stream()
			.anyMatch(order -> "12 Nguyễn Huệ, Phường Ba Đình, Thành phố Hà Nội".equals(order.getShippingAddress())));
	}

	@Test
	void loginPageRendersWithoutSocialProvidersConfigured() throws Exception {
		mockMvc.perform(get("/account/login"))
			.andExpect(status().isOk())
			.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
				.string(containsString("name=\"email\"")))
			.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
				.string(containsString("Tiếp tục với Google")))
			.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
				.string(containsString("Tiếp tục với Apple")))
			.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
				.string(org.hamcrest.Matchers.not(containsString("/oauth2/authorization/google"))))
			.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
				.string(org.hamcrest.Matchers.not(containsString("/oauth2/authorization/apple"))));
	}

	@Test
	void adminPagesRequireAuthentication() throws Exception {
		mockMvc.perform(get("/admin"))
			.andExpect(status().is3xxRedirection())
			.andExpect(header().string("Location", containsString("/account/login")));
	}

	@Test
	void customerCannotAccessAdminPages() throws Exception {
		mockMvc.perform(get("/admin").with(user("customer").authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"))))
			.andExpect(status().isForbidden());
	}

	@Test
	void administratorCanAccessDashboard() throws Exception {
		mockMvc.perform(get("/admin").with(user("admin").authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
			.andExpect(status().isOk());
	}

	@Test
	void administratorCanOpenNewProductForm() throws Exception {
		mockMvc.perform(get("/admin/products/new").with(user("admin").authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
			.andExpect(status().isOk())
			.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
				.string(containsString("name=\"images\"")))
			.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
				.string(containsString(" multiple")));
	}

	@Test
	void administratorCanCreateProduct() throws Exception {
		mockMvc.perform(post("/admin/products/save")
				.with(user("admin").authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
				.with(csrf())
				.param("name", testProductName)
				.param("categoryId", "1")
				.param("price", "125000")
				.param("quantity", "8")
				.param("status", "Đang bán")
				.param("size", "M")
				.param("color", "Be")
				.param("description", "Sản phẩm kiểm thử"))
			.andExpect(status().is3xxRedirection())
			.andExpect(header().string("Location", "/admin/products"));

		org.junit.jupiter.api.Assertions.assertTrue(products.findAll().stream()
			.anyMatch(product -> product.getName().equals(testProductName)));
	}

	@Test
	void administratorCanUploadAndViewMultipleProductImages() throws Exception {
		MockMultipartFile firstImage = new MockMultipartFile("images", "front.png", "image/png", new byte[]{1, 2, 3});
		MockMultipartFile secondImage = new MockMultipartFile("images", "back.jpg", "image/jpeg", new byte[]{4, 5, 6});

		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
				.multipart("/admin/products/save")
				.file(firstImage)
				.file(secondImage)
				.with(user("admin").authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
				.with(csrf())
				.param("name", testProductName)
				.param("categoryId", "1")
				.param("price", "125000")
				.param("quantity", "8")
				.param("status", "Đang bán")
				.param("size", "M")
				.param("color", "Be")
				.param("description", "Sản phẩm có nhiều ảnh"))
			.andExpect(status().is3xxRedirection())
			.andExpect(header().string("Location", "/admin/products"));

		var savedProduct = products.findAll().stream()
			.filter(product -> product.getName().equals(testProductName))
			.findFirst().orElseThrow();
		org.junit.jupiter.api.Assertions.assertEquals(2, savedProduct.getImageUrls().size());

		try {
			mockMvc.perform(get("/products/{id}", savedProduct.getId()))
				.andExpect(status().isOk())
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
					.string(containsString("detail-thumbnail")));
		} finally {
			for (String imageUrl : savedProduct.getImageUrls()) {
				if (imageUrl.startsWith("/uploads/")) {
					java.nio.file.Files.deleteIfExists(java.nio.file.Path.of("uploads",
						imageUrl.substring("/uploads/".length())));
				}
			}
		}
	}

	@Test
	void changingOrderStatusNotifiesCustomerAndAdministrator() throws Exception {
		AppUser customer = users.findByEmailIgnoreCase("khach1@example.com").orElseThrow();
		AppUser admin = users.findByEmailIgnoreCase("admin@macstudio.vn").orElseThrow();
		var order = new vn.macstudio.macstudio.model.CustomerOrder();
		order.setUser(customer);
		order.setRecipientName(customer.getFullName());
		order.setShippingAddress(customer.getAddress());
		order.setPhone(customer.getPhone());
		order.setPaymentMethod(vn.macstudio.macstudio.model.PaymentMethod.COD);
		order.setStatus(OrderStatus.PENDING);
		order.setTotalAmount(java.math.BigDecimal.valueOf(1000));
		order = orders.saveAndFlush(order);

		mockMvc.perform(post("/admin/orders/{id}/status", order.getId())
				.with(user(admin))
				.with(csrf())
				.param("status", OrderStatus.CONFIRMED.name()))
			.andExpect(status().is3xxRedirection())
			.andExpect(header().string("Location", "/admin/orders"));

		org.junit.jupiter.api.Assertions.assertEquals(1, notifications.countByRecipientIdAndReadFalse(customer.getId()));
		org.junit.jupiter.api.Assertions.assertEquals(1, notifications.countByRecipientIdAndReadFalse(admin.getId()));
	}

	@Test
	void newOrderNotifiesAdministrator() throws Exception {
		AppUser customer = users.findByEmailIgnoreCase("khach1@example.com").orElseThrow();
		AppUser admin = users.findByEmailIgnoreCase("admin@macstudio.vn").orElseThrow();
		var product = products.findAll().getFirst();
		CheckoutForm form = new CheckoutForm();
		form.setRecipientName(customer.getFullName());
		form.setPhone(customer.getPhone());
		form.setAddress(customer.getAddress());
		form.setPaymentMethod(vn.macstudio.macstudio.model.PaymentMethod.COD);
		orderService.placeOrder(customer.getEmail(), form, java.util.List.of(
			new CartLine(product.getId(), product.getName(), product.getImageUrl(), product.getPrice(), product.getSize(), product.getColor(), 1)));

		mockMvc.perform(get("/notifications").with(user(admin)))
			.andExpect(status().isOk())
			.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
				.string(containsString("cần xác nhận")));
	}

	@Test
	void customerCanOpenChatAndSendMessageToAdministrators() throws Exception {
		AppUser customer = users.findByEmailIgnoreCase("khach1@example.com").orElseThrow();
		AppUser admin = users.findByEmailIgnoreCase("admin@macstudio.vn").orElseThrow();

		mockMvc.perform(get("/chat").with(user(customer)))
			.andExpect(status().isOk())
			.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
				.string(containsString("Chat với Mặc Studio")));

		mockMvc.perform(post("/chat/messages")
				.with(user(customer))
				.with(csrf())
				.param("content", "Tôi cần hỗ trợ"))
			.andExpect(status().isOk())
			.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.content")
				.value("Tôi cần hỗ trợ"));

		mockMvc.perform(get("/chat").with(user(admin)))
			.andExpect(status().isOk())
			.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
				.string(containsString("Tôi cần hỗ trợ")));

		mockMvc.perform(post("/chat/messages")
				.with(user(admin))
				.with(csrf())
				.param("customerId", customer.getId().toString())
				.param("content", "Chúng tôi sẵn sàng hỗ trợ"))
			.andExpect(status().isOk())
			.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.ownMessage")
				.value(true));

		mockMvc.perform(get("/chat/messages").with(user(customer)))
			.andExpect(status().isOk())
			.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
				.string(containsString("Chúng tôi sẵn sàng hỗ trợ")));
	}

	@Test
	void customerCannotAccessAnotherCustomersChat() throws Exception {
		AppUser customer = users.findByEmailIgnoreCase("khach1@example.com").orElseThrow();
		AppUser otherCustomer = users.findByEmailIgnoreCase("khach2@example.com").orElseThrow();

		mockMvc.perform(get("/chat/messages")
				.with(user(customer))
				.param("customerId", otherCustomer.getId().toString()))
			.andExpect(status().isForbidden());
	}

	@Test
	void greetingIsCreatedForFirstCustomerChatAndUnreadMessagesNotifyAdministrator() throws Exception {
		AppUser customer = users.findByEmailIgnoreCase("khach1@example.com").orElseThrow();
		AppUser admin = users.findByEmailIgnoreCase("admin@macstudio.vn").orElseThrow();

		mockMvc.perform(get("/chat").with(user(customer)))
			.andExpect(status().isOk())
			.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
				.string(containsString("Cảm ơn bạn đã liên hệ Mặc Studio")));

		mockMvc.perform(post("/chat/messages")
				.with(user(customer))
				.with(csrf())
				.param("content", "Tôi cần tư vấn sản phẩm"))
			.andExpect(status().isOk());

		mockMvc.perform(get("/chat").with(user(admin)))
			.andExpect(status().isOk())
			.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
				.string(containsString("class=\"notification-count chat-header-count")))
			.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
				.string(containsString("/js/chat-notifications.js")));

		mockMvc.perform(get("/chat/unread-count").with(user(admin)))
			.andExpect(status().isOk())
			.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string("1"));

		mockMvc.perform(get("/chat/{customerId}", customer.getId()).with(user(admin)))
			.andExpect(status().isOk())
			.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
				.string(containsString("Tôi cần tư vấn sản phẩm")));

		mockMvc.perform(post("/chat/messages")
				.with(user(admin))
				.with(csrf())
				.param("customerId", customer.getId().toString())
				.param("content", "Chúng tôi sẽ phản hồi sớm"))
			.andExpect(status().isOk());

		mockMvc.perform(get("/").with(user(customer)))
			.andExpect(status().isOk())
			.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
				.string(containsString("class=\"notification-count chat-header-count")));
		mockMvc.perform(get("/chat/unread-count").with(user(customer)))
			.andExpect(status().isOk())
			.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string("1"));

		mockMvc.perform(get("/chat").with(user(customer)))
			.andExpect(status().isOk())
			.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
				.string(containsString("Chúng tôi sẽ phản hồi sớm")));

		mockMvc.perform(get("/chat/unread-count").with(user(customer)))
			.andExpect(status().isOk())
			.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string("0"));

		mockMvc.perform(get("/chat/unread-count").with(user(admin)))
			.andExpect(status().isOk())
			.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string("0"));
	}
}
