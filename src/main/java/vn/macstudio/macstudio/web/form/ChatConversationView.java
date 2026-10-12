package vn.macstudio.macstudio.web.form;

public record ChatConversationView(Long customerId, String customerName, String lastMessage, String lastMessageAt,
                                  long unreadCount) {}
