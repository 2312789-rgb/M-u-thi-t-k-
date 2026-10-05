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
import vn.macstudio.macstudio.repository.ProductRepository;
import vn.macstudio.macstudio.repository.AppUserRepository;
import vn.macstudio.macstudio.repository.CustomerOrderRepository;
import vn.macstudio.macstudio.repository.UserNotificationRepository;
import vn.macstudio.macstudio.model.AppUser;
import vn.macstudio.macstudio.model.OrderStatus;
import vn.macstudio.macstudio.service.OrderService;
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

	private String testProductName;

	@BeforeEach
	void setUp() {
		testProductName = "Áo test " + System.nanoTime();
	}

	@Test
	void contextLoads() {
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
			.andExpect(status().isOk());
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
}
