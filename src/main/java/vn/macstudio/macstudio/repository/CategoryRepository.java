package vn.macstudio.macstudio.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.macstudio.macstudio.model.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findAllByOrderByNameAsc();
}