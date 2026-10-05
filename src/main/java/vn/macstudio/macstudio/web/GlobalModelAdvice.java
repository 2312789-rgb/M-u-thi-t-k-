package vn.macstudio.macstudio.web;

import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import vn.macstudio.macstudio.service.CartService;
import vn.macstudio.macstudio.service.NotificationService;
import vn.macstudio.macstudio.service.StoreMetadata;
import vn.macstudio.macstudio.model.AppUser;

@ControllerAdvice
public class GlobalModelAdvice {
    private final CartService cart;
    private final StoreMetadata store;
    private final NotificationService notifications;
    public GlobalModelAdvice(CartService cart, StoreMetadata store, NotificationService notifications) {
        this.cart = cart; this.store = store; this.notifications = notifications;
    }

    @ModelAttribute("storeName") public String storeName() { return store.getName(); }
    @ModelAttribute("cartCount") public int cartCount(HttpSession session) { return cart.get(session).stream().mapToInt(line -> line.quantity()).sum(); }
    @ModelAttribute("currentRole") public String currentRole(Authentication authentication) {
        return authentication == null || !authentication.isAuthenticated() ? "" : authentication.getAuthorities().stream().map(a -> a.getAuthority()).filter(a -> a.startsWith("ROLE_")).findFirst().orElse("");
    }
    @ModelAttribute("notificationCount") public long notificationCount(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AppUser user)) return 0;
        return notifications.unreadCount(user.getId());
    }
}