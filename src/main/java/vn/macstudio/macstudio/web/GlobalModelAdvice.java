package vn.macstudio.macstudio.web;

import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import vn.macstudio.macstudio.service.CartService;
import vn.macstudio.macstudio.service.ChatService;
import vn.macstudio.macstudio.service.NotificationService;
import vn.macstudio.macstudio.service.SocialLoginSettings;
import vn.macstudio.macstudio.service.StoreMetadata;
import vn.macstudio.macstudio.model.AppUser;

@ControllerAdvice
public class GlobalModelAdvice {
    private final CartService cart;
    private final ChatService chat;
    private final SocialLoginSettings socialLogin;
    private final StoreMetadata store;
    private final NotificationService notifications;
    public GlobalModelAdvice(CartService cart, ChatService chat, SocialLoginSettings socialLogin,
                             StoreMetadata store, NotificationService notifications) {
        this.cart = cart; this.chat = chat; this.socialLogin = socialLogin; this.store = store; this.notifications = notifications;
    }

    @ModelAttribute("storeName") public String storeName() { return store.getName(); }
    @ModelAttribute("googleLoginEnabled") public boolean googleLoginEnabled() { return socialLogin.isGoogleEnabled(); }
    @ModelAttribute("appleLoginEnabled") public boolean appleLoginEnabled() { return socialLogin.isAppleEnabled(); }
    @ModelAttribute("cartCount") public int cartCount(HttpSession session) { return cart.get(session).stream().mapToInt(line -> line.quantity()).sum(); }
    @ModelAttribute("currentRole") public String currentRole(Authentication authentication) {
        return authentication == null || !authentication.isAuthenticated() ? "" : authentication.getAuthorities().stream().map(a -> a.getAuthority()).filter(a -> a.startsWith("ROLE_")).findFirst().orElse("");
    }
    @ModelAttribute("notificationCount") public long notificationCount(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AppUser user)) return 0;
        return notifications.unreadCount(user.getId());
    }
    @ModelAttribute("chatUnreadCount") public long chatUnreadCount(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AppUser user)
            || (user.getRole() != vn.macstudio.macstudio.model.Role.ADMIN
                && user.getRole() != vn.macstudio.macstudio.model.Role.CUSTOMER)) return 0;
        return chat.unreadMessageCount(user);
    }
}