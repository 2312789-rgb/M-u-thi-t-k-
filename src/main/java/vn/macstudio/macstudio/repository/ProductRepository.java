package vn.macstudio.macstudio.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.macstudio.macstudio.model.Product;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    @Query("select p from Product p join fetch p.category where p.status = 'Đang bán' and (:q is null or lower(p.name) like lower(concat('%', :q, '%'))) and (:categoryId is null or p.category.id = :categoryId) and (:size is null or p.size = :size) and (:color is null or p.color = :color) and (:min is null or p.price >= :min) and (:max is null or p.price <= :max) and (:availableOnly = false or p.quantity > 0) order by case when :sort = 'price-asc' then p.price end asc, case when :sort = 'price-desc' then p.price end desc, case when :sort = 'best-selling' then p.soldCount end desc, case when :sort is null or :sort = 'newest' then p.createdDate end desc")
    List<Product> search(@Param("q") String q, @Param("categoryId") Long categoryId, @Param("size") String size,
                         @Param("color") String color, @Param("min") BigDecimal min, @Param("max") BigDecimal max,
                         @Param("availableOnly") boolean availableOnly, @Param("sort") String sort);

    List<Product> findTop5ByOrderBySoldCountDesc();
    long countByQuantityLessThanEqual(int quantity);
    boolean existsByCategoryId(Long categoryId);
    boolean existsByIdAndQuantityGreaterThan(Long id, int quantity);
    List<Product> findAllByCreatedDateAfterOrderByCreatedDateDesc(LocalDateTime from);
}