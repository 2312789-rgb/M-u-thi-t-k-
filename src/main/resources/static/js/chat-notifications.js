(() => {
    const badge = document.querySelector("[data-chat-header-count]");
    if (!badge) return;

    async function refreshUnreadCount() {
        try {
            const response = await fetch("/chat/unread-count", { headers: { Accept: "application/json" } });
            if (!response.ok) throw new Error("Không thể tải số tin nhắn chưa đọc.");
            const count = await response.json();
            badge.textContent = count > 99 ? "99+" : String(count);
            badge.classList.toggle("is-empty", count === 0);
        } catch {
            badge.setAttribute("aria-label", "Không thể cập nhật số tin nhắn chưa đọc");
        }
    }

    window.setInterval(refreshUnreadCount, 4000);
})();
