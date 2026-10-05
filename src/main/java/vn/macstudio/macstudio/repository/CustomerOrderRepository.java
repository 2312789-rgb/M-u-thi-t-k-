package vn.macstudio.macstudio.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.macstudio.macstudio.model.CustomerOrder;
import vn.macstudio.macstudio.model.OrderStatus;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {
    List<CustomerOrder> findAllByUserEmailIgnoreCaseOrderByOrderDateDesc(String email);
    List<CustomerOrder> findAllByOrderByOrderDateDesc();
    List<CustomerOrder> findTop5ByUserEmailIgnoreCaseOrderByOrderDateDesc(String email);
    Optional<CustomerOrder> findByIdAndUserEmailIgnoreCase(Long id, String email);
    long countByOrderDateGreaterThanEqual(LocalDateTime from);
    @Query("select coalesce(sum(o.totalAmount), 0) from CustomerOrder o where o.status = :status")
    BigDecimal revenueByStatus(@Param("status") OrderStatus status);
    @Query("select coalesce(sum(o.totalAmount), 0) from CustomerOrder o where o.status <> :status and o.orderDate >= :from")
    BigDecimal revenueSince(@Param("from") LocalDateTime from, @Param("status") OrderStatus excludedStatus);
    List<CustomerOrder> findAllByStatus(OrderStatus status);
    boolean existsByUserId(Long userId);
}