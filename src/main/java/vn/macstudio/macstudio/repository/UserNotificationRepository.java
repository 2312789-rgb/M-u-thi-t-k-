package vn.macstudio.macstudio.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.macstudio.macstudio.model.UserNotification;

public interface UserNotificationRepository extends JpaRepository<UserNotification, Long> {
    List<UserNotification> findAllByRecipientIdOrderByCreatedAtDesc(Long recipientId);
    long countByRecipientIdAndReadFalse(Long recipientId);
    Optional<UserNotification> findByIdAndRecipientId(Long id, Long recipientId);
    List<UserNotification> findAllByRecipientIdAndReadFalse(Long recipientId);
}
