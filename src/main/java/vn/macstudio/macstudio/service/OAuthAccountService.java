package vn.macstudio.macstudio.service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.macstudio.macstudio.model.AppUser;
import vn.macstudio.macstudio.model.OAuthAccount;
import vn.macstudio.macstudio.model.Role;
import vn.macstudio.macstudio.repository.AppUserRepository;
import vn.macstudio.macstudio.repository.OAuthAccountRepository;

@Service
public class OAuthAccountService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final OAuthAccountRepository oauthAccounts;
    private final AppUserRepository users;
    private final PasswordEncoder passwords;

    public OAuthAccountService(OAuthAccountRepository oauthAccounts, AppUserRepository users, PasswordEncoder passwords) {
        this.oauthAccounts = oauthAccounts;
        this.users = users;
        this.passwords = passwords;
    }

    @Transactional
    public AppUser findOrCreateCustomer(String provider, String subject, String email, String displayName, boolean emailVerified) {
        if (!"google".equals(provider) && !"apple".equals(provider))
            throw new IllegalArgumentException("Nhà cung cấp đăng nhập không được hỗ trợ.");
        if (subject == null || subject.isBlank() || subject.length() > 200)
            throw new IllegalArgumentException("Nhà cung cấp không trả về mã tài khoản hợp lệ.");
        var linkedAccount = oauthAccounts.findByProviderAndProviderSubject(provider, subject);
        if (linkedAccount.isPresent()) {
            AppUser linkedUser = linkedAccount.get().getUser();
            if (linkedUser.getRole() != Role.CUSTOMER)
                throw new AccessDeniedException("Tài khoản quản trị không thể đăng nhập bằng Google hoặc Apple.");
            return linkedUser;
        }
        if (!emailVerified) throw new AccessDeniedException("Nhà cung cấp chưa xác nhận email của bạn.");
        if (email == null || email.isBlank() || email.length() > 254 || !email.contains("@"))
            throw new AccessDeniedException("Nhà cung cấp không trả về email hợp lệ.");

        String normalizedEmail = email.strip().toLowerCase(Locale.ROOT);
        AppUser user = linkOrCreateCustomer(provider, subject, normalizedEmail, displayName);
        if (user.getRole() != Role.CUSTOMER)
            throw new AccessDeniedException("Tài khoản quản trị không thể đăng nhập bằng Google hoặc Apple.");
        return user;
    }

    private AppUser linkOrCreateCustomer(String provider, String subject, String email, String displayName) {
        AppUser user = users.findByEmailIgnoreCase(email).orElseGet(() -> createCustomer(email, displayName));
        if (user.getRole() != Role.CUSTOMER)
            throw new AccessDeniedException("Không thể liên kết tài khoản quản trị với đăng nhập Google hoặc Apple.");
        oauthAccounts.saveAndFlush(new OAuthAccount(user, provider, subject));
        return user;
    }

    private AppUser createCustomer(String email, String displayName) {
        String name = displayName == null || displayName.isBlank()
            ? email.substring(0, email.indexOf('@'))
            : displayName.strip();
        if (name.length() > 100) name = name.substring(0, 100);
        byte[] randomPassword = new byte[48];
        RANDOM.nextBytes(randomPassword);
        String encodedPassword = passwords.encode(Base64.getUrlEncoder().withoutPadding().encodeToString(randomPassword));
        return users.saveAndFlush(new AppUser(name, email, encodedPassword, null, "", Role.CUSTOMER));
    }
}
