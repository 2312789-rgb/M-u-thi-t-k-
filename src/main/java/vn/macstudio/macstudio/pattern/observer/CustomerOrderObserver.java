package vn.macstudio.macstudio.pattern.observer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import vn.macstudio.macstudio.model.CustomerOrder;
import vn.macstudio.macstudio.model.OrderStatus;
import vn.macstudio.macstudio.service.NotificationService;

@Component
public class CustomerOrderObserver implements OrderStatusObserver {
    private static final Logger log = LoggerFactory.getLogger(CustomerOrderObserver.class);
    private final NotificationService notifications;

    public CustomerOrderObserver(NotificationService notifications) {
        this.notifications = notifications;
    }

    @Override public void onStatusChanged(CustomerOrder order, OrderStatus previousStatus) {
        log.info("Customer notification: order {} changed from {} to {}", order.getId(), previousStatus, order.getStatus());
        notifications.notifyCustomerStatusChanged(order, previousStatus);
    }
}