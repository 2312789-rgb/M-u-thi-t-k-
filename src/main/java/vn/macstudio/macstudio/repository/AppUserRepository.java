package vn.macstudio.macstudio.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.macstudio.macstudio.model.AppUser;
import vn.macstudio.macstudio.model.Role;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmailIgnoreCase(String email);
    List<AppUser> findAllByRoleOrderByCreatedDateDesc(Role role);
    long countByRole(Role role);
}