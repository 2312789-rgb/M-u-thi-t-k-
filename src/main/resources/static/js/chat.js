(() => {
    const panel = document.querySelector(".chat-panel");
    const messageList = document.querySelector("#chat-messages");
    const form = document.querySelector("#chat-compose");
    const status = document.querySelector("#chat-status");
    if (!panel || !messageList || !form || !status) return;

    const customerId = panel.dataset.customerId;
    const messagesUrl = customerId ? `/chat/messages?customerId=${encodeURIComponent(customerId)}` : "/chat/messages";
    let latestMessages = "";

    function renderMessages(messages) {
        const serialized = JSON.stringify(messages);
        if (serialized === latestMessages) return;
        const shouldScroll = messageList.scrollHeight - messageList.scrollTop - messageList.clientHeight < 80;
        latestMessages = serialized;
        messageList.replaceChildren();
        if (messages.length === 0) {
            const empty = document.createElement("p");
            empty.className = "chat-empty";
            empty.textContent = "Chưa có tin nhắn. Hãy gửi lời chào để bắt đầu.";
            messageList.append(empty);
        }
        for (const message of messages) {
            const article = document.createElement("article");
            article.className = `chat-message${message.ownMessage ? " chat-message-own" : ""}`;
            const sender = document.createElement("strong");
            sender.textContent = message.senderName;
            const content = document.createElement("p");
            content.textContent = message.content;
            const time = document.createElement("time");
            time.textContent = message.sentAt;
            article.append(sender, content, time);
            messageList.append(article);
        }
        if (shouldScroll) messageList.scrollTop = messageList.scrollHeight;
    }

    async function refreshMessages() {
        try {
            const response = await fetch(messagesUrl, { headers: { Accept: "application/json" } });
            if (!response.ok) throw new Error("Không thể tải tin nhắn.");
            renderMessages(await response.json());
            status.textContent = "";
        } catch {
            status.textContent = "Không tải được tin nhắn. Đang thử kết nối lại...";
        }
    }

    form.addEventListener("submit", async event => {
        event.preventDefault();
        const button = form.querySelector("button[type=submit]");
        const content = form.elements.content;
        const formData = new URLSearchParams(new FormData(form));
        button.disabled = true;
        status.textContent = "";
        try {
            const response = await fetch(form.action, {
                method: "POST",
                headers: { "Content-Type": "application/x-www-form-urlencoded", Accept: "application/json" },
                body: formData
            });
            const result = await response.json();
            if (!response.ok) throw new Error(result.message || "Không gửi được tin nhắn.");
            content.value = "";
            await refreshMessages();
            content.focus();
        } catch (error) {
            status.textContent = error instanceof Error ? error.message : "Không gửi được tin nhắn.";
        } finally {
            button.disabled = false;
        }
    });

    renderMessages(Array.from(messageList.querySelectorAll(".chat-message"), element => ({
        senderName: element.querySelector("strong").textContent,
        content: element.querySelector("p").textContent,
        sentAt: element.querySelector("time").textContent,
        ownMessage: element.classList.contains("chat-message-own")
    })));
    messageList.scrollTop = messageList.scrollHeight;
    window.setInterval(refreshMessages, 4000);
})();
