package vn.macstudio.macstudio.web;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import vn.macstudio.macstudio.model.AppUser;
import vn.macstudio.macstudio.model.Role;
import vn.macstudio.macstudio.service.ChatService;
import vn.macstudio.macstudio.web.form.ChatConversationView;
import vn.macstudio.macstudio.web.form.ChatMessageView;

@Controller
@RequestMapping("/chat")
public class ChatController {
    private final ChatService chat;

    public ChatController(ChatService chat) {
        this.chat = chat;
    }

    @GetMapping
    public String index(@AuthenticationPrincipal AppUser user, Model model) {
        if (user.getRole() == Role.ADMIN) {
            model.addAttribute("conversations", chat.findConversations(user));
            return "chat/inbox";
        }
        chat.startCustomerConversation(user);
        model.addAttribute("messages", chat.findMessages(user, null));
        model.addAttribute("customerName", user.getFullName());
        model.addAttribute("customerId", user.getId());
        return "chat/conversation";
    }

    @GetMapping("/{customerId}")
    public String conversation(@AuthenticationPrincipal AppUser user, @org.springframework.web.bind.annotation.PathVariable Long customerId,
                               Model model) {
        requireAdmin(user);
        List<ChatMessageView> messages = chat.findMessages(user, customerId);
        List<ChatConversationView> conversations = chat.findConversations(user);
        model.addAttribute("messages", messages);
        model.addAttribute("customerId", customerId);
        model.addAttribute("customerName", conversations.stream()
            .filter(conversation -> conversation.customerId().equals(customerId))
            .map(conversation -> conversation.customerName())
            .findFirst()
            .orElseGet(() -> messages.isEmpty() ? "Khách hàng" : messages.getFirst().senderName()));
        model.addAttribute("conversations", conversations);
        return "chat/conversation";
    }

    @GetMapping("/conversations")
    @ResponseBody
    public List<ChatConversationView> conversations(@AuthenticationPrincipal AppUser user) {
        if (user.getRole() != Role.ADMIN) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return chat.findConversations(user);
    }

    @GetMapping("/unread-count")
    @ResponseBody
    public long unreadCount(@AuthenticationPrincipal AppUser user) {
        if (user.getRole() != Role.ADMIN && user.getRole() != Role.CUSTOMER)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return chat.unreadMessageCount(user);
    }

    @GetMapping("/messages")
    @ResponseBody
    public List<ChatMessageView> messages(@AuthenticationPrincipal AppUser user,
                                          @RequestParam(required = false) Long customerId) {
        return chat.findMessages(user, customerId);
    }

    @PostMapping("/messages")
    @ResponseBody
    public ResponseEntity<?> send(@AuthenticationPrincipal AppUser user,
                                  @RequestParam(required = false) Long customerId,
                                  @RequestParam String content) {
        try {
            return ResponseEntity.ok(chat.send(user, customerId, content));
        } catch (AccessDeniedException exception) {
            throw exception;
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(new ChatError(exception.getMessage()));
        }
    }

    private void requireAdmin(AppUser user) {
        if (user.getRole() != Role.ADMIN) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

    private record ChatError(String message) {}
}
