package vn.macstudio.macstudio.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.macstudio.macstudio.model.AppUser;
import vn.macstudio.macstudio.model.CustomerOrder;
import vn.macstudio.macstudio.model.OrderStatus;
import vn.macstudio.macstudio.model.Role;
import vn.macstudio.macstudio.model.UserNotification;
import vn.macstudio.macstudio.repository.AppUserRepository;
import vn.macstudio.macstudio.repository.UserNotificationRepository;

@Service
public class NotificationService {
    private final UserNotificationRepository notifications;
    private final AppUserRepository users;

    public NotificationService(UserNotificationRepository notifications, AppUserRepository users) {
        this.notifications = notifications;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<UserNotification> findForUser(Long userId) {
        return notifications.findAllByRecipientIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return notifications.countByRecipientIdAndReadFalse(userId);
    }

    @Transactional
    public Long markRead(Long notificationId, Long userId) {
        UserNotification notification = notifications.findByIdAndRecipientId(notificationId, userId)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông báo."));
        notification.markRead();
        return notification.getOrderId();
    }

    @Transactional
    public void markAllRead(Long userId) {
        notifications.findAllByRecipientIdAndReadFalse(userId).forEach(UserNotification::markRead);
    }

    @Transactional
    public void notifyCustomerStatusChanged(CustomerOrder order, OrderStatus previousStatus) {
        AppUser customer = users.findById(order.getUser().getId())
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khách hàng của đơn hàng."));
        create(customer, "Đơn hàng #" + orderNumber(order) + " đã chuyển từ "
            + previousStatus.vietnamese() + " sang " + order.getStatus().vietnamese() + ".", order);
    }

    @Transactional
    public void notifyAdminsStatusChanged(CustomerOrder order, OrderStatus previousStatus) {
        String message = "Đơn hàng #" + orderNumber(order) + " của " + order.getUser().getFullName()
            + " đã chuyển từ " + previousStatus.vietnamese() + " sang " + order.getStatus().vietnamese() + ".";
        notifyRole(Role.ADMIN, message, order);
    }

    @Transactional
    public void notifyAdminsNewOrder(CustomerOrder order) {
        String message = "Có đơn hàng mới #" + orderNumber(order) + " từ " + order.getUser().getFullName()
            + " cần xác nhận.";
        notifyRole(Role.ADMIN, message, order);
    }

    private void notifyRole(Role role, String message, CustomerOrder order) {
        users.findAllByRoleOrderByCreatedDateDesc(role)
            .forEach(user -> create(user, message, order));
    }

    private void create(AppUser recipient, String message, CustomerOrder order) {
        notifications.save(new UserNotification(recipient, message, order.getId()));
    }

    private String orderNumber(CustomerOrder order) {
        return String.format("%05d", order.getId());
    }
}
