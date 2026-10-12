package vn.macstudio.macstudio.service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.macstudio.macstudio.model.AppUser;
import vn.macstudio.macstudio.model.ChatMessage;
import vn.macstudio.macstudio.model.Role;
import vn.macstudio.macstudio.repository.AppUserRepository;
import vn.macstudio.macstudio.repository.ChatMessageRepository;
import vn.macstudio.macstudio.web.form.ChatConversationView;
import vn.macstudio.macstudio.web.form.ChatMessageView;

@Service
public class ChatService {
    private static final int MAX_MESSAGE_LENGTH = 2000;
    private static final DateTimeFormatter SENT_AT_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ChatMessageRepository messages;
    private final AppUserRepository users;

    public ChatService(ChatMessageRepository messages, AppUserRepository users) {
        this.messages = messages;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<ChatConversationView> findConversations(AppUser viewer) {
        requireRole(viewer, Role.ADMIN);
        return messages.findLatestMessageForEachCustomer().stream()
            .map(message -> new ChatConversationView(
                message.getCustomer().getId(),
                message.getCustomer().getFullName(),
                message.getContent(),
                message.getCreatedAt().format(SENT_AT_FORMAT),
                messages.countUnreadByCustomerIdAndSenderRole(message.getCustomer().getId(), Role.CUSTOMER)))
            .toList();
    }

    @Transactional
    public List<ChatMessageView> findMessages(AppUser viewer, Long customerId) {
        AppUser customer = resolveCustomer(viewer, customerId);
        List<ChatMessageView> conversation = messages.findAllByCustomerIdOrderByCreatedAtAscIdAsc(customer.getId()).stream()
            .map(message -> toView(message, viewer))
            .toList();
        Role senderRole = viewer.getRole() == Role.ADMIN ? Role.CUSTOMER : Role.ADMIN;
        messages.markUnreadByCustomerIdAndSenderRoleRead(customer.getId(), senderRole);
        return conversation;
    }

    @Transactional
    public void startCustomerConversation(AppUser customer) {
        requireRole(customer, Role.CUSTOMER);
        if (messages.existsByCustomerId(customer.getId())) return;
        AppUser admin = users.findAllByRoleOrderByCreatedDateDesc(Role.ADMIN).stream()
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Không tìm thấy tài khoản quản trị để gửi lời chào."));
        ChatMessage greeting = new ChatMessage(customer, admin,
            "Xin chào " + customer.getFullName() + "! Cảm ơn bạn đã liên hệ Mặc Studio. "
                + "Bạn cứ để lại tin nhắn, đội ngũ quản trị sẽ phản hồi sớm nhất có thể.");
        greeting.markRead();
        messages.saveAndFlush(greeting);
    }

    @Transactional(readOnly = true)
    public long unreadMessageCount(AppUser viewer) {
        if (viewer.getRole() == Role.ADMIN) return messages.countUnreadBySenderRole(Role.CUSTOMER);
        requireRole(viewer, Role.CUSTOMER);
        return messages.countUnreadByCustomerIdAndSenderRole(viewer.getId(), Role.ADMIN);
    }

    @Transactional
    public ChatMessageView send(AppUser viewer, Long customerId, String content) {
        AppUser customer = resolveCustomer(viewer, customerId);
        if (content == null || content.isBlank()) throw new IllegalArgumentException("Vui lòng nhập nội dung tin nhắn.");
        String normalizedContent = content.strip();
        if (normalizedContent.length() > MAX_MESSAGE_LENGTH)
            throw new IllegalArgumentException("Tin nhắn không được vượt quá 2000 ký tự.");
        return toView(messages.saveAndFlush(new ChatMessage(customer, viewer, normalizedContent)), viewer);
    }

    private AppUser resolveCustomer(AppUser viewer, Long customerId) {
        if (viewer.getRole() == Role.CUSTOMER) {
            if (customerId != null && !customerId.equals(viewer.getId()))
                throw new AccessDeniedException("Bạn chỉ có thể xem cuộc trò chuyện của mình.");
            return viewer;
        }
        requireRole(viewer, Role.ADMIN);
        if (customerId == null) throw new IllegalArgumentException("Cần chọn cuộc trò chuyện với khách hàng.");
        AppUser customer = users.findById(customerId)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khách hàng."));
        if (customer.getRole() != Role.CUSTOMER) throw new IllegalArgumentException("Tài khoản được chọn không phải khách hàng.");
        return customer;
    }

    private void requireRole(AppUser viewer, Role role) {
        if (viewer.getRole() != role) throw new AccessDeniedException("Bạn không có quyền truy cập cuộc trò chuyện này.");
    }

    private ChatMessageView toView(ChatMessage message, AppUser viewer) {
        return new ChatMessageView(
            message.getSender().getFullName(),
            message.getContent(),
            message.getCreatedAt().format(SENT_AT_FORMAT),
            message.getSender().getId().equals(viewer.getId()));
    }
}
