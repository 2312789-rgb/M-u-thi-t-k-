package vn.macstudio.macstudio.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.macstudio.macstudio.model.AppUser;
import vn.macstudio.macstudio.model.OAuthAccount;
import vn.macstudio.macstudio.model.Role;
import vn.macstudio.macstudio.repository.AppUserRepository;
import vn.macstudio.macstudio.repository.OAuthAccountRepository;

class OAuthAccountServiceTest {
    private final OAuthAccountRepository identities = mock(OAuthAccountRepository.class);
    private final AppUserRepository users = mock(AppUserRepository.class);
    private final PasswordEncoder passwords = mock(PasswordEncoder.class);
    private OAuthAccountService service;

    @BeforeEach
    void setUp() {
        service = new OAuthAccountService(identities, users, passwords);
        when(passwords.encode(any(String.class))).thenReturn("encoded-random-password");
    }

    @Test
    void linksProviderToExistingCustomerWhenEmailIsVerified() {
        AppUser customer = customer("customer@example.com", Role.CUSTOMER);
        when(identities.findByProviderAndProviderSubject("google", "google-sub"))
            .thenReturn(Optional.empty());
        when(users.findByEmailIgnoreCase("customer@example.com")).thenReturn(Optional.of(customer));
        when(identities.saveAndFlush(any(OAuthAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppUser result = service.findOrCreateCustomer("google", "google-sub", "Customer@example.com", "Customer", true);

        assertEquals(customer, result);
        verify(identities).saveAndFlush(any(OAuthAccount.class));
        verify(users, never()).saveAndFlush(any(AppUser.class));
    }

    @Test
    void createsNewCustomerForVerifiedSocialAccount() {
        when(identities.findByProviderAndProviderSubject("apple", "apple-sub")).thenReturn(Optional.empty());
        when(users.findByEmailIgnoreCase("customer@example.com")).thenReturn(Optional.empty());
        when(users.saveAndFlush(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(identities.saveAndFlush(any(OAuthAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppUser result = service.findOrCreateCustomer("apple", "apple-sub", " Customer@example.com ", null, true);

        assertEquals("customer", result.getFullName());
        assertEquals("customer@example.com", result.getEmail());
        assertEquals(Role.CUSTOMER, result.getRole());
        verify(identities).saveAndFlush(any(OAuthAccount.class));
    }

    @Test
    void refusesUnverifiedEmailWithoutCreatingAnAccount() {
        assertThrows(AccessDeniedException.class, () ->
            service.findOrCreateCustomer("google", "google-sub", "customer@example.com", "Customer", false));

        verify(users, never()).saveAndFlush(any(AppUser.class));
        verify(identities, never()).saveAndFlush(any(OAuthAccount.class));
    }

    @Test
    void returnsPreviouslyLinkedAppleAccountWhenProviderOmitsEmailOnLaterLogin() {
        AppUser customer = customer("customer@example.com", Role.CUSTOMER);
        OAuthAccount identity = new OAuthAccount(customer, "apple", "apple-sub");
        when(identities.findByProviderAndProviderSubject("apple", "apple-sub"))
            .thenReturn(Optional.of(identity));

        AppUser result = service.findOrCreateCustomer("apple", "apple-sub", null, null, false);

        assertEquals(customer, result);
        verify(identities, never()).saveAndFlush(any(OAuthAccount.class));
        verify(users, never()).findByEmailIgnoreCase(any(String.class));
    }

    @Test
    void doesNotLinkSocialLoginToAdministratorAccount() {
        AppUser admin = customer("admin@example.com", Role.ADMIN);
        when(identities.findByProviderAndProviderSubject("google", "google-sub"))
            .thenReturn(Optional.empty());
        when(users.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(admin));

        assertThrows(AccessDeniedException.class, () ->
            service.findOrCreateCustomer("google", "google-sub", "admin@example.com", "Admin", true));

        verify(identities, never()).saveAndFlush(any(OAuthAccount.class));
    }

    private AppUser customer(String email, Role role) {
        return new AppUser("Customer", email, "hash", null, "", role);
    }
}
