package vn.macstudio.macstudio.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.macstudio.macstudio.model.OAuthAccount;

public interface OAuthAccountRepository extends JpaRepository<OAuthAccount, Long> {
    Optional<OAuthAccount> findByProviderAndProviderSubject(String provider, String providerSubject);
}
