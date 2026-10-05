package vn.macstudio.macstudio.service;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import vn.macstudio.macstudio.model.AppUser;
import vn.macstudio.macstudio.model.CustomerOrder;
import vn.macstudio.macstudio.model.OrderItem;
import vn.macstudio.macstudio.model.OrderStatus;
import vn.macstudio.macstudio.model.Product;
import vn.macstudio.macstudio.model.Role;
import vn.macstudio.macstudio.pattern.observer.OrderCreatedObserver;
import vn.macstudio.macstudio.pattern.observer.OrderStatusObserver;
import vn.macstudio.macstudio.pattern.state.OrderState;
import vn.macstudio.macstudio.repository.AppUserRepository;
import vn.macstudio.macstudio.repository.CustomerOrderRepository;
import vn.macstudio.macstudio.repository.ProductRepository;
import vn.macstudio.macstudio.web.form.CartLine;
import vn.macstudio.macstudio.web.form.CheckoutForm;

@Service
public class OrderService {
    private final ProductRepository products;
    private final CustomerOrderRepository orders;
    private final AppUserRepository users;
    private final List<OrderCreatedObserver> orderCreatedObservers;
    private final List<OrderStatusObserver> orderStatusObservers;

    public OrderService(ProductRepository products, CustomerOrderRepository orders, AppUserRepository users,
                        List<OrderCreatedObserver> orderCreatedObservers, List<OrderStatusObserver> orderStatusObservers) {
        this.products = products;
        this.orders = orders;
        this.users = users;
        this.orderCreatedObservers = List.copyOf(orderCreatedObservers);
        this.orderStatusObservers = List.copyOf(orderStatusObservers);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public CustomerOrder placeOrder(String email, CheckoutForm form, List<CartLine> cart) {
        if (cart.isEmpty()) throw new IllegalArgumentException("Giỏ hàng đang trống.");
        AppUser user = users.findByEmailIgnoreCase(email).orElseThrow(() -> new IllegalArgumentException("Tài khoản không tồn tại."));
        if (user.getRole() != Role.CUSTOMER) throw new IllegalArgumentException("Chỉ khách hàng mới có thể đặt hàng.");
        CustomerOrder order = new CustomerOrder();
        order.setUser(user);
        order.setRecipientName(form.getRecipientName().trim());
        order.setPhone(form.getPhone().trim());
        order.setShippingAddress(form.getAddress().trim());
        order.setPaymentMethod(form.getPaymentMethod());
        order.setNote(form.getNote());
        BigDecimal total = BigDecimal.ZERO;
        for (CartLine line : cart) {
            Product product = products.findById(line.productId()).orElseThrow(() -> new IllegalArgumentException("Một sản phẩm trong giỏ không còn tồn tại."));
            if (!"Đang bán".equals(product.getStatus()) || product.getQuantity() < line.quantity())
                throw new IllegalArgumentException("Sản phẩm " + product.getName() + " không còn đủ số lượng. Giỏ hàng chưa được đặt.");
            product.setQuantity(product.getQuantity() - line.quantity());
            product.setSoldCount(product.getSoldCount() + line.quantity());
            OrderItem item = new OrderItem(product, line.quantity(), product.getPrice());
            order.addItem(item);
            total = total.add(item.getSubTotal());
        }
        order.setTotalAmount(total);
        CustomerOrder savedOrder = orders.saveAndFlush(order);
        orderCreatedObservers.forEach(observer -> observer.onOrderCreated(savedOrder));
        return savedOrder;
    }

    @Transactional
    public CustomerOrder changeStatus(long orderId, OrderStatus target) {
        CustomerOrder order = orders.findById(orderId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng."));
        OrderStatus previous = order.getStatus();
        if (!OrderState.from(previous).canTransitionTo(target))
            throw new IllegalArgumentException("Trạng thái mới không hợp lệ với tiến trình đơn hàng.");
        if (target == OrderStatus.CANCELLED) {
            for (OrderItem item : order.getItems()) {
                Product product = item.getProduct();
                product.setQuantity(product.getQuantity() + item.getQuantity());
                product.setSoldCount(Math.max(0, product.getSoldCount() - item.getQuantity()));
            }
        }
        order.setStatus(target);
        CustomerOrder savedOrder = orders.save(order);
        orderStatusObservers.forEach(observer -> observer.onStatusChanged(savedOrder, previous));
        return savedOrder;
    }
}