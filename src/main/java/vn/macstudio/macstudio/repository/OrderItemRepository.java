package vn.macstudio.macstudio.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.macstudio.macstudio.model.OrderItem;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    boolean existsByProductId(Long productId);
}