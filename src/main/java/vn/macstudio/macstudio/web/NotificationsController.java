package vn.macstudio.macstudio.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.macstudio.macstudio.model.AppUser;
import vn.macstudio.macstudio.model.Role;
import vn.macstudio.macstudio.service.NotificationService;

@Controller
@RequestMapping("/notifications")
public class NotificationsController {
    private final NotificationService notifications;

    public NotificationsController(NotificationService notifications) {
        this.notifications = notifications;
    }

    @GetMapping
    public String index(@AuthenticationPrincipal AppUser user, Model model) {
        model.addAttribute("notifications", notifications.findForUser(user.getId()));
        return "notifications/index";
    }

    @PostMapping("/{id}/read")
    public String open(@PathVariable long id, @AuthenticationPrincipal AppUser user, RedirectAttributes flash) {
        Long orderId;
        try {
            orderId = notifications.markRead(id, user.getId());
        } catch (IllegalArgumentException exception) {
            flash.addFlashAttribute("error", exception.getMessage());
            return "redirect:/notifications";
        }
        return user.getRole() == Role.ADMIN
            ? "redirect:/admin/orders"
            : "redirect:/orders/" + orderId;
    }

    @PostMapping("/read-all")
    public String markAllRead(@AuthenticationPrincipal AppUser user, RedirectAttributes flash) {
        notifications.markAllRead(user.getId());
        flash.addFlashAttribute("success", "Đã đánh dấu tất cả thông báo là đã đọc.");
        return "redirect:/notifications";
    }
}
