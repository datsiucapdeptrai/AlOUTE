document.addEventListener("DOMContentLoaded", function () {
    const sidebar = document.getElementById("chatSidebar");
    if (!sidebar) return;

    const contacts = document.getElementById("chatContacts");
    const search = document.getElementById("chatSearch");
    const popup = document.getElementById("chatWindow");
    const messages = document.getElementById("chatMessages");
    const scroll = document.getElementById("chatScroll");
    const olderButton = document.getElementById("chatOlderButton");
    const errorBox = document.getElementById("chatError");
    const composer = document.getElementById("chatComposer");
    const input = document.getElementById("chatInput");
    const sendButton = document.getElementById("chatSendButton");
    const backdrop = document.getElementById("chatBackdrop");
    const launcher = document.getElementById("chatLauncher");

    let friends = [];
    let unreadCounts = {};
    const previews = new Map();
    let filter = "all";
    let activeFriend = null;
    let currentPage = 0;
    let hasOlder = false;
    let historyLoading = false;
    let historyGeneration = 0;
    let contactsGeneration = 0;
    let pendingMessages = [];
    let lastReadId = 0;
    let readTimer;
    let contactError = "";
    const shownIds = new Set();

    const baseUrl = sidebar.dataset.messageUrl;
    const currentUserId = sidebar.dataset.userId;
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

    function postHeaders() {
        const headers = { "Accept": "application/json" };
        if (csrfToken && csrfHeader) headers[csrfHeader] = csrfToken;
        return headers;
    }

    async function readJson(response) {
        if (!(response.headers.get("content-type") || "")
                .includes("application/json")) {
            throw new Error("Máy chủ chưa trả dữ liệu chat. Hãy kiểm tra DirectMessageController.");
        }
        const data = await response.json();
        if (!response.ok) throw new Error(data.message || "Không thể tải dữ liệu chat.");
        return data;
    }

    function firstLetter(name) {
        return Array.from((name || "?").trim())[0] || "?";
    }

    function displayTime(value) {
        const date = new Date(value);
        return Number.isNaN(date.getTime())
            ? (value || "")
            : date.toLocaleString("vi-VN", {
                day: "2-digit", month: "2-digit", hour: "2-digit", minute: "2-digit"
            });
    }

    function setError(message) {
        errorBox.textContent = message || "";
        errorBox.classList.toggle("d-none", !message);
    }

    function openSidebar() {
        if (window.innerWidth >= 1200) {
            search.focus();
            return;
        }
        sidebar.classList.add("is-open");
        backdrop.classList.remove("d-none");
        search.focus();
    }

    function closeSidebar() {
        sidebar.classList.remove("is-open");
        backdrop.classList.add("d-none");
    }

    function renderContacts() {
        contacts.replaceChildren();
        const keyword = search.value.trim().toLocaleLowerCase("vi-VN");
        const visible = [...friends]
            .sort((a, b) =>
                (Number(unreadCounts[b.userId]) || 0) -
                (Number(unreadCounts[a.userId]) || 0) ||
                (a.fullName || a.username || "").localeCompare(
                    b.fullName || b.username || "", "vi-VN"
                )
            )
            .filter(friend => {
                const matches = (friend.fullName || "").toLocaleLowerCase("vi-VN")
                    .includes(keyword) || (friend.username || "").toLocaleLowerCase("vi-VN")
                    .includes(keyword);
                return matches && (filter === "all" || Number(unreadCounts[friend.userId]) > 0);
            });

        if (visible.length === 0) {
            const empty = document.createElement("p");
            empty.className = "text-center small p-3 mb-0";
            empty.textContent = contactError || (keyword
                ? "Không tìm thấy bạn bè."
                : filter === "unread"
                    ? "Không có tin nhắn chưa đọc."
                    : "Chưa có bạn bè để trò chuyện.");
            contacts.appendChild(empty);
            return;
        }

        const fragment = document.createDocumentFragment();
        for (const friend of visible) {
            const button = document.createElement("button");
            button.type = "button";
            button.className = "chat-contact";
            button.dataset.friendId = String(friend.userId);

            const avatar = document.createElement("span");
            avatar.className = "chat-avatar";
            avatar.textContent = firstLetter(friend.fullName || friend.username);

            const details = document.createElement("span");
            details.className = "chat-contact-details";
            const name = document.createElement("strong");
            name.className = "d-block text-truncate";
            name.textContent = friend.fullName || friend.username;
            const preview = document.createElement("span");
            preview.className = "chat-preview";
            preview.textContent = previews.get(String(friend.userId)) ||
                "@" + friend.username + " · Nhấn để trò chuyện";
            details.append(name, preview);
            button.append(avatar, details);

            const count = Number(unreadCounts[friend.userId]) || 0;
            if (count > 0) {
                const badge = document.createElement("span");
                badge.className = "chat-unread small";
                badge.textContent = String(count);
                badge.setAttribute("aria-label", count + " tin chưa đọc");
                button.appendChild(badge);
            }
            button.addEventListener("click", () => openConversation(friend));
            fragment.appendChild(button);
        }
        contacts.appendChild(fragment);
    }

    async function loadContacts() {
        const request = ++contactsGeneration;
        try {
            const response = await fetch(sidebar.dataset.contactsUrl, {
                headers: { "Accept": "application/json" }, credentials: "same-origin"
            });
            const data = await readJson(response);
            if (request !== contactsGeneration) return;
            friends = Array.isArray(data.friends) ? data.friends : [];
            unreadCounts = data.unreadCounts || {};
            contactError = "";
            renderContacts();
        } catch (error) {
            if (request !== contactsGeneration) return;
            contactError = error.message;
            console.error(error);
            renderContacts();
        }
    }

    function makeMessage(message) {
        const id = String(message.id);
        if (shownIds.has(id)) return null;
        shownIds.add(id);

        const own = String(message.senderId) === currentUserId;
        const row = document.createElement("div");
        row.className = "chat-message" + (own ? " own" : "");
        row.dataset.messageId = id;
        row.dataset.own = String(own);
        const bubble = document.createElement("div");
        bubble.className = "chat-bubble";
        const content = document.createElement("div");
        content.textContent = message.content;
        const time = document.createElement("small");
        time.className = "chat-message-meta";
        time.textContent = displayTime(message.createdAt);
        bubble.append(content, time);

        if (own) {
            const status = document.createElement("small");
            status.className = "chat-message-meta read-status";
            status.textContent = message.read || Number(id) <= lastReadId
                ? "Đã xem" : "Đã gửi";
            bubble.appendChild(status);
        }
        row.appendChild(bubble);
        return row;
    }

    function appendMessage(message) {
        const element = makeMessage(message);
        if (!element) return;
        messages.appendChild(element);
        scroll.scrollTop = scroll.scrollHeight;
        if (activeFriend) {
            previews.set(String(activeFriend.userId), message.content);
            renderContacts();
        }
    }

    async function loadHistory(page, generation) {
        if (!activeFriend || historyLoading) return;
        historyLoading = true;
        const friendId = activeFriend.userId;
        olderButton.disabled = true;
        setError("");
        const previousHeight = scroll.scrollHeight;
        const previousTop = scroll.scrollTop;

        try {
            const url = sidebar.dataset.historyUrl + "/" +
                encodeURIComponent(friendId) + "?page=" + page;
            const response = await fetch(url, {
                headers: { "Accept": "application/json" }, credentials: "same-origin"
            });
            const data = await readJson(response);
            if (generation !== historyGeneration) return;

            const oldestFirst = [...data.messages].reverse();
            const fragment = document.createDocumentFragment();
            oldestFirst.forEach(message => {
                const element = makeMessage(message);
                if (element) fragment.appendChild(element);
            });

            if (page === 0) {
                messages.replaceChildren(fragment);
                if (data.messages.length > 0) {
                    previews.set(String(friendId), data.messages[0].content);
                    renderContacts();
                }
            } else {
                messages.prepend(fragment);
            }

            currentPage = data.page;
            hasOlder = Boolean(data.hasOlder);
            olderButton.classList.toggle("d-none", !hasOlder);
            if (page === 0) {
                pendingMessages.forEach(message => appendMessage(message));
                pendingMessages = [];
                scroll.scrollTop = scroll.scrollHeight;
                scheduleRead(friendId);
            } else {
                scroll.scrollTop = previousTop + scroll.scrollHeight - previousHeight;
            }
        } catch (error) {
            if (generation === historyGeneration) setError(error.message);
        } finally {
            if (generation === historyGeneration) {
                historyLoading = false;
                olderButton.disabled = false;
                if (page !== 0) {
                    pendingMessages.forEach(message => appendMessage(message));
                    pendingMessages = [];
                }
            }
        }
    }

    function openConversation(friend) {
        activeFriend = friend;
        historyGeneration++;
        historyLoading = false;
        currentPage = 0;
        hasOlder = false;
        lastReadId = 0;
        pendingMessages = [];
        shownIds.clear();
        messages.replaceChildren();
        olderButton.classList.add("d-none");
        setError("");

        document.getElementById("chatPartnerName").textContent =
            friend.fullName || friend.username;
        document.getElementById("chatPartnerAvatar").textContent =
            firstLetter(friend.fullName || friend.username);
        document.getElementById("chatFullLink").href =
            baseUrl + "/" + encodeURIComponent(friend.userId);
        popup.classList.remove("d-none");
        closeSidebar();
        input.focus();
        loadHistory(0, historyGeneration);
    }

    function closeConversation() {
        historyGeneration++;
        activeFriend = null;
        popup.classList.add("d-none");
        clearTimeout(readTimer);
    }

    function scheduleRead(friendId) {
        if (document.visibilityState !== "visible" || popup.classList.contains("d-none"))
            return;
        clearTimeout(readTimer);
        readTimer = setTimeout(async function () {
            if (!activeFriend || String(activeFriend.userId) !== String(friendId) ||
                    document.visibilityState !== "visible") return;
            try {
                const response = await fetch(
                    baseUrl + "/" + encodeURIComponent(friendId) + "/read",
                    { method: "POST", headers: postHeaders(), credentials: "same-origin" }
                );
                if (!response.ok) throw new Error("Không thể đánh dấu tin đã đọc.");
                unreadCounts[friendId] = 0;
                renderContacts();
                document.dispatchEvent(new Event("fakebook:refresh-unread"));
            } catch (error) {
                console.error(error);
            }
        }, 300);
    }

    composer.addEventListener("submit", async function (event) {
        event.preventDefault();
        if (!activeFriend || !input.value.trim()) return;
        const friendId = activeFriend.userId;
        const content = input.value.trim();
        sendButton.disabled = true;
        setError("");

        try {
            const response = await fetch(baseUrl + "/" + encodeURIComponent(friendId), {
                method: "POST", credentials: "same-origin",
                headers: {
                    ...postHeaders(), "X-Requested-With": "XMLHttpRequest"
                },
                body: new URLSearchParams({ content })
            });
            const data = await readJson(response);
            if (activeFriend && String(activeFriend.userId) === String(friendId)) {
                if (historyLoading) pendingMessages.push(data);
                else appendMessage(data);
            }
            if (input.value.trim() === content) input.value = "";
            input.focus();
        } catch (error) {
            setError(error.message);
        } finally {
            sendButton.disabled = false;
        }
    });

    olderButton.addEventListener("click", function () {
        if (!historyLoading && hasOlder) loadHistory(currentPage + 1, historyGeneration);
    });
    document.getElementById("chatWindowClose").addEventListener("click", closeConversation);
    document.getElementById("chatSidebarClose").addEventListener("click", closeSidebar);
    backdrop.addEventListener("click", closeSidebar);
    launcher.addEventListener("click", openSidebar);
    search.addEventListener("input", renderContacts);
    document.querySelectorAll("[data-chat-filter]").forEach(button => {
        button.addEventListener("click", function () {
            filter = button.dataset.chatFilter;
            document.querySelectorAll("[data-chat-filter]").forEach(item => {
                item.classList.toggle("active", item === button);
            });
            renderContacts();
        });
    });

    document.getElementById("chatNavbarButton").addEventListener("click", function (event) {
        if (event.button !== 0 || event.ctrlKey || event.metaKey || event.shiftKey)
            return;
        event.preventDefault();
        openSidebar();
    });

    document.addEventListener("fakebook:incoming-message", function (event) {
        const message = event.detail;
        if (!message || message.id == null || message.senderId == null) return;
        const senderId = String(message.senderId);
        previews.set(senderId, message.content);

        if (activeFriend && String(activeFriend.userId) === senderId &&
                !popup.classList.contains("d-none")) {
            if (historyLoading) pendingMessages.push(message);
            else appendMessage(message);
            if (document.visibilityState === "visible") scheduleRead(senderId);
            else unreadCounts[senderId] = (Number(unreadCounts[senderId]) || 0) + 1;
        } else {
            unreadCounts[senderId] = (Number(unreadCounts[senderId]) || 0) + 1;
        }
        renderContacts();
    });

    document.addEventListener("fakebook:message-read", function (event) {
        const receipt = event.detail;
        if (!activeFriend || String(receipt?.readerId) !== String(activeFriend.userId))
            return;
        const id = Number(receipt.readUpToId);
        if (!Number.isSafeInteger(id)) return;
        lastReadId = Math.max(lastReadId, id);
        messages.querySelectorAll('[data-own="true"]').forEach(row => {
            if (Number(row.dataset.messageId) <= lastReadId) {
                const status = row.querySelector(".read-status");
                if (status) status.textContent = "Đã xem";
            }
        });
    });

    document.addEventListener("visibilitychange", function () {
        if (document.visibilityState === "visible") {
            loadContacts();
            if (activeFriend) scheduleRead(activeFriend.userId);
        }
    });
    window.addEventListener("focus", loadContacts);
    window.addEventListener("resize", function () {
        if (window.innerWidth >= 1200) closeSidebar();
    });

    renderContacts();
    loadContacts();
});
