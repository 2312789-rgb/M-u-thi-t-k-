(() => {
    const inboxes = document.querySelectorAll("[data-chat-inbox]");
    if (inboxes.length === 0) return;

    async function refreshConversations() {
        try {
            const response = await fetch("/chat/conversations", { headers: { Accept: "application/json" } });
            if (!response.ok) throw new Error("Không thể tải danh sách cuộc trò chuyện.");
            const conversations = await response.json();
            for (const inbox of inboxes) {
                const activeHref = window.location.pathname;
                inbox.replaceChildren();
                if (conversations.length === 0) {
                    const empty = document.createElement("div");
                    empty.className = "empty-state";
                    const title = document.createElement("h2");
                    title.textContent = "Chưa có cuộc trò chuyện";
                    const description = document.createElement("p");
                    description.textContent = "Tin nhắn từ khách hàng sẽ xuất hiện tại đây.";
                    empty.append(title, description);
                    inbox.append(empty);
                    continue;
                }
                for (const conversation of conversations) {
                    const link = document.createElement("a");
                    link.className = "chat-conversation-link";
                    link.href = `/chat/${encodeURIComponent(conversation.customerId)}`;
                    if (activeHref === link.pathname) link.classList.add("chat-conversation-active");
                    const avatar = document.createElement("span");
                    avatar.className = "chat-avatar";
                    const icon = document.createElement("i");
                    icon.className = "fa-regular fa-user";
                    avatar.append(icon);
                    const copy = document.createElement("span");
                    copy.className = "chat-conversation-copy";
                    const name = document.createElement("strong");
                    name.textContent = conversation.customerName;
                    const preview = document.createElement("span");
                    preview.textContent = conversation.lastMessage;
                    copy.append(name, preview);
                    link.append(avatar, copy);
                    if (conversation.unreadCount > 0) {
                        const unread = document.createElement("span");
                        unread.className = "chat-unread-badge";
                        unread.textContent = conversation.unreadCount > 99 ? "99+" : conversation.unreadCount;
                        link.append(unread);
                    }
                    const time = document.createElement("time");
                    time.textContent = conversation.lastMessageAt;
                    link.append(time);
                    inbox.append(link);
                }
            }
        } catch {
            const pageStatus = document.querySelector("#chat-status");
            if (pageStatus) pageStatus.textContent = "Không tải được danh sách tin nhắn. Đang thử kết nối lại...";
        }
    }

    window.setInterval(refreshConversations, 4000);
})();
