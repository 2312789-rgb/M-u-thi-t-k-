package vn.macstudio.macstudio.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.macstudio.macstudio.model.ChatMessage;
import vn.macstudio.macstudio.model.Role;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findAllByCustomerIdOrderByCreatedAtAscIdAsc(Long customerId);

    boolean existsByCustomerId(Long customerId);

    @Query("""
        select count(message) from ChatMessage message
        where message.sender.role = :role
          and (message.read = false or message.read is null)
        """)
    long countUnreadBySenderRole(@Param("role") Role role);

    @Query("""
        select count(message) from ChatMessage message
        where message.customer.id = :customerId
          and message.sender.role = :role
          and (message.read = false or message.read is null)
        """)
    long countUnreadByCustomerIdAndSenderRole(@Param("customerId") Long customerId, @Param("role") Role role);

    @Modifying
    @Query("""
        update ChatMessage message set message.read = true
        where message.customer.id = :customerId
          and message.sender.role = :role
          and (message.read = false or message.read is null)
        """)
    int markUnreadByCustomerIdAndSenderRoleRead(@Param("customerId") Long customerId, @Param("role") Role role);

    @Query("""
        select message from ChatMessage message
        where message.id in (
            select max(latest.id) from ChatMessage latest group by latest.customer.id
        )
        order by message.id desc
        """)
    List<ChatMessage> findLatestMessageForEachCustomer();
}
