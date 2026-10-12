package vn.macstudio.macstudio.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import vn.macstudio.macstudio.model.AppUser;
import vn.macstudio.macstudio.model.ChatMessage;
import vn.macstudio.macstudio.model.Role;
import vn.macstudio.macstudio.repository.AppUserRepository;
import vn.macstudio.macstudio.repository.ChatMessageRepository;

class ChatServiceTest {
    private final ChatMessageRepository messages = mock(ChatMessageRepository.class);
    private final AppUserRepository users = mock(AppUserRepository.class);
    private ChatService service;

    @BeforeEach
    void setUp() {
        service = new ChatService(messages, users);
    }

    @Test
    void customerCanSendMessageToSharedAdminInbox() {
        AppUser customer = user(7L, Role.CUSTOMER);
        when(messages.saveAndFlush(org.mockito.ArgumentMatchers.any(ChatMessage.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.send(customer, null, "  Xin chào  ");

        assertEquals("Xin chào", result.content());
        assertEquals("Khách hàng", result.senderName());
        assertEquals(true, result.ownMessage());
        verify(messages).saveAndFlush(org.mockito.ArgumentMatchers.any(ChatMessage.class));
    }

    @Test
    void customerCannotReadAnotherCustomersConversation() {
        AppUser customer = user(7L, Role.CUSTOMER);

        assertThrows(AccessDeniedException.class, () -> service.findMessages(customer, 8L));
    }

    @Test
    void blankMessageIsRejected() {
        AppUser customer = user(7L, Role.CUSTOMER);

        assertThrows(IllegalArgumentException.class, () -> service.send(customer, null, "  "));
    }

    @Test
    void startsConversationWithGreetingOnlyOnce() {
        AppUser customer = user(7L, Role.CUSTOMER);
        AppUser admin = user(8L, Role.ADMIN);
        when(messages.existsByCustomerId(7L)).thenReturn(false, true);
        when(users.findAllByRoleOrderByCreatedDateDesc(Role.ADMIN)).thenReturn(List.of(admin));
        when(messages.saveAndFlush(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage saved = invocation.getArgument(0);
            assertEquals(true, saved.isRead());
            return saved;
        });

        service.startCustomerConversation(customer);
        service.startCustomerConversation(customer);
        verify(messages).saveAndFlush(any(ChatMessage.class));
        verify(messages).saveAndFlush(any(ChatMessage.class));
    }

    @Test
    void adminReadingConversationMarksCustomerMessagesRead() {
        AppUser customer = user(7L, Role.CUSTOMER);
        AppUser admin = user(8L, Role.ADMIN);
        when(users.findById(7L)).thenReturn(java.util.Optional.of(customer));
        when(messages.findAllByCustomerIdOrderByCreatedAtAscIdAsc(7L))
            .thenReturn(List.of(new ChatMessage(customer, customer, "Cần hỗ trợ")));

        var result = service.findMessages(admin, 7L);

        assertEquals(1, result.size());
        assertEquals(false, result.getFirst().ownMessage());
        verify(messages).markUnreadByCustomerIdAndSenderRoleRead(7L, Role.CUSTOMER);
    }

    @Test
    void customerReadingConversationMarksAdminMessagesRead() {
        AppUser customer = user(7L, Role.CUSTOMER);
        AppUser admin = user(8L, Role.ADMIN);
        when(messages.findAllByCustomerIdOrderByCreatedAtAscIdAsc(7L))
            .thenReturn(List.of(new ChatMessage(customer, admin, "Chúng tôi sẽ hỗ trợ bạn")));

        var result = service.findMessages(customer, null);

        assertEquals(1, result.size());
        assertEquals(false, result.getFirst().ownMessage());
        verify(messages).markUnreadByCustomerIdAndSenderRoleRead(7L, Role.ADMIN);
    }

    private AppUser user(long id, Role role) {
        AppUser user = new AppUser("Khách hàng", "customer@example.com", "hash", "0900000000", "Địa chỉ", role);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
