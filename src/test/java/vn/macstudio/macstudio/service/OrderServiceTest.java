package vn.macstudio.macstudio.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.macstudio.macstudio.model.AppUser;
import vn.macstudio.macstudio.model.CustomerOrder;
import vn.macstudio.macstudio.model.OrderItem;
import vn.macstudio.macstudio.model.OrderStatus;
import vn.macstudio.macstudio.model.PaymentMethod;
import vn.macstudio.macstudio.model.Product;
import vn.macstudio.macstudio.model.Role;
import vn.macstudio.macstudio.repository.AppUserRepository;
import vn.macstudio.macstudio.repository.CustomerOrderRepository;
import vn.macstudio.macstudio.repository.ProductRepository;
import vn.macstudio.macstudio.web.form.CartLine;
import vn.macstudio.macstudio.web.form.CheckoutForm;

class OrderServiceTest {
    private final ProductRepository products = mock(ProductRepository.class);
    private final CustomerOrderRepository orders = mock(CustomerOrderRepository.class);
    private final AppUserRepository users = mock(AppUserRepository.class);
    private OrderService service;

    @BeforeEach
    void setUp() {
        service = new OrderService(products, orders, users, java.util.List.of(), java.util.List.of());
    }

    @Test
    void cancellingOrderRestoresInventoryAndMarksOrderCancelled() {
        Product product = new Product();
        product.setQuantity(3);
        product.setSoldCount(2);
        CustomerOrder order = orderWithItem(product);
        when(orders.findById(1L)).thenReturn(Optional.of(order));
        when(orders.save(order)).thenReturn(order);

        CustomerOrder updated = service.changeStatus(1L, OrderStatus.CANCELLED);

        assertEquals(OrderStatus.CANCELLED, updated.getStatus());
        assertEquals(5, product.getQuantity());
        assertEquals(0, product.getSoldCount());
        verify(orders).save(order);
    }

    @Test
    void rejectsInvalidTransitionWithoutSavingOrChangingInventory() {
        Product product = new Product();
        product.setQuantity(3);
        product.setSoldCount(2);
        CustomerOrder order = orderWithItem(product);
        when(orders.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(IllegalArgumentException.class, () -> service.changeStatus(1L, OrderStatus.DELIVERED));

        assertEquals(OrderStatus.PENDING, order.getStatus());
        assertEquals(3, product.getQuantity());
        assertEquals(2, product.getSoldCount());
        verify(orders, never()).save(order);
    }

    @Test
    void placesOrderAndUpdatesInventoryUsingCurrentProductPrice() {
        AppUser customer = new AppUser("Khách hàng", "customer@example.com", "hash", "0900000000", "12 Nguyễn Huệ, Quận 1", Role.CUSTOMER);
        Product product = availableProduct(5, 125000);
        when(users.findByEmailIgnoreCase("customer@example.com")).thenReturn(Optional.of(customer));
        when(products.findById(7L)).thenReturn(Optional.of(product));
        when(orders.saveAndFlush(any(CustomerOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerOrder order = service.placeOrder("customer@example.com", checkoutForm(),
            List.of(new CartLine(7L, "Áo sơ mi", "/image.jpg", BigDecimal.ONE, "M", "Be", 2)));

        assertEquals(OrderStatus.PENDING, order.getStatus());
        assertEquals(PaymentMethod.COD, order.getPaymentMethod());
        assertEquals(BigDecimal.valueOf(250000), order.getTotalAmount());
        assertEquals(BigDecimal.valueOf(125000), order.getItems().getFirst().getUnitPrice());
        assertEquals(3, product.getQuantity());
        assertEquals(2, product.getSoldCount());
        verify(orders).saveAndFlush(order);
    }

    @Test
    void rejectsOrderWhenStockIsInsufficientWithoutChangingInventory() {
        AppUser customer = new AppUser("Khách hàng", "customer@example.com", "hash", "0900000000", "12 Nguyễn Huệ, Quận 1", Role.CUSTOMER);
        Product product = availableProduct(1, 125000);
        when(users.findByEmailIgnoreCase("customer@example.com")).thenReturn(Optional.of(customer));
        when(products.findById(7L)).thenReturn(Optional.of(product));

        assertThrows(IllegalArgumentException.class, () -> service.placeOrder("customer@example.com", checkoutForm(),
            List.of(new CartLine(7L, "Áo sơ mi", "/image.jpg", BigDecimal.ONE, "M", "Be", 2))));

        assertEquals(1, product.getQuantity());
        assertEquals(0, product.getSoldCount());
        verify(orders, never()).saveAndFlush(any(CustomerOrder.class));
    }

    @Test
    void preventsNonCustomersFromPlacingOrders() {
        AppUser admin = new AppUser("Quản trị", "admin@example.com", "hash", "0900000000", "12 Nguyễn Huệ, Quận 1", Role.ADMIN);
        when(users.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(admin));

        assertThrows(IllegalArgumentException.class, () -> service.placeOrder("admin@example.com", checkoutForm(),
            List.of(new CartLine(7L, "Áo sơ mi", "/image.jpg", BigDecimal.ONE, "M", "Be", 1))));

        verify(products, never()).findById(7L);
        verify(orders, never()).saveAndFlush(any(CustomerOrder.class));
    }

    private CustomerOrder orderWithItem(Product product) {
        CustomerOrder order = new CustomerOrder();
        order.setStatus(OrderStatus.PENDING);
        order.addItem(new OrderItem(product, 2, BigDecimal.valueOf(1000)));
        return order;
    }

    private Product availableProduct(int quantity, long price) {
        Product product = new Product();
        product.setName("Áo sơ mi");
        product.setStatus("Đang bán");
        product.setPrice(BigDecimal.valueOf(price));
        product.setQuantity(quantity);
        product.setSoldCount(0);
        return product;
    }

    private CheckoutForm checkoutForm() {
        CheckoutForm form = new CheckoutForm();
        form.setRecipientName("Nguyễn An");
        form.setPhone("0900000000");
        form.setAddress("12 Nguyễn Huệ, Quận 1");
        form.setPaymentMethod(PaymentMethod.COD);
        return form;
    }
}
