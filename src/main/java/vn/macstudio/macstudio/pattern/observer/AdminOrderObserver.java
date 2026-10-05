package vn.macstudio.macstudio.pattern.observer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import vn.macstudio.macstudio.model.CustomerOrder;
import vn.macstudio.macstudio.model.OrderStatus;
import vn.macstudio.macstudio.service.NotificationService;

@Component
public class AdminOrderObserver implements OrderStatusObserver, OrderCreatedObserver {
    private static final Logger log = LoggerFactory.getLogger(AdminOrderObserver.class);
    private final NotificationService notifications;

    public AdminOrderObserver(NotificationService notifications) {
        this.notifications = notifications;
    }

    @Override public void onStatusChanged(CustomerOrder order, OrderStatus previousStatus) {
        log.info("Admin notification: order {} changed to {}", order.getId(), order.getStatus());
        notifications.notifyAdminsStatusChanged(order, previousStatus);
    }

    @Override public void onOrderCreated(CustomerOrder order) {
        log.info("Admin notification: new order {}", order.getId());
        notifications.notifyAdminsNewOrder(order);
    }
}