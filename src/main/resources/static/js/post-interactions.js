(function () {
    "use strict";

    // ============ REACTION ============
    document.addEventListener("click", async function (event) {
        const btn = event.target.closest(".reaction-btn, .reaction-main-btn");
        if (!btn) return;

        const wrapper = btn.closest(".reaction-wrapper");
        if (!wrapper) return;

        const mainBtn = wrapper.querySelector(".reaction-main-btn");
        const postId = mainBtn.dataset.postId;
        const currentReaction = mainBtn.dataset.userReaction || "";

        let reactionType;
        if (btn.classList.contains("reaction-btn")) {
            reactionType = btn.dataset.reaction;
        } else {
            // Bấm nút chính: nếu chưa có reaction → LIKE, nếu có → toggle off
            reactionType = currentReaction || "LIKE";
        }

        try {
            const formData = new URLSearchParams();
            formData.append("reactionType", reactionType);

            const response = await fetch(`/posts/${postId}/reaction`, {
                method: "POST",
                headers: {
                    "X-Requested-With": "XMLHttpRequest",
                    "Content-Type": "application/x-www-form-urlencoded"
                },
                body: formData
            });

            if (response.status === 401 || response.status === 403) {
                alert("Vui lòng đăng nhập để thả cảm xúc.");
                return;
            }

            const data = await response.json();
            if (!data.success) throw new Error(data.message);

            // Cập nhật UI
            const iconSpan = mainBtn.querySelector(".reaction-icon");
            const labelSpan = mainBtn.querySelector(".reaction-label");
            const countSpan = document.getElementById(`like-count-${postId}`);

            if (data.removed) {
                mainBtn.dataset.userReaction = "";
                mainBtn.classList.remove("btn-primary");
                mainBtn.classList.add("btn-outline-primary");
                iconSpan.textContent = "👍";
                labelSpan.textContent = "Thích";
            } else {
                const map = {
                    LIKE:  { icon: "👍", label: "Thích",     cls: "btn-primary" },
                    LOVE:  { icon: "❤️", label: "Yêu thích", cls: "btn-danger"  },
                    HAHA:  { icon: "😆", label: "Haha",      cls: "btn-warning" },
                    WOW:   { icon: "😮", label: "Wow",       cls: "btn-warning" },
                    SAD:   { icon: "😢", label: "Buồn",      cls: "btn-warning" },
                    ANGRY: { icon: "😡", label: "Phẫn nộ",   cls: "btn-danger"  }
                };
                const info = map[data.reactionType] || map.LIKE;
                mainBtn.dataset.userReaction = data.reactionType;
                mainBtn.classList.remove("btn-outline-primary", "btn-primary",
                                          "btn-danger", "btn-warning");
                mainBtn.classList.add(info.cls);
                iconSpan.textContent = info.icon;
                labelSpan.textContent = info.label;
            }

            if (countSpan) countSpan.textContent = data.totalCount;

        } catch (error) {
            alert(error.message || "Không thể thả cảm xúc.");
        }
    });

    // ============ COMMENT ============
    document.addEventListener("submit", async function (event) {
        const form = event.target;
        if (!(form instanceof HTMLFormElement) || !form.classList.contains("comment-form")) return;

        event.preventDefault();
        const button = form.querySelector('button[type="submit"]');
        const input = form.querySelector('input[name="content"]');
        const list = document.getElementById("comments-post-" + form.dataset.postId);
        if (!input.value.trim()) return;
        button.disabled = true;

        try {
            const response = await fetch(form.action, {
                method: "POST",
                body: new FormData(form),
                headers: { "Accept": "application/json", "X-Requested-With": "XMLHttpRequest" }
            });
            if (!(response.headers.get("content-type") || "").includes("application/json")) {
                throw new Error("Máy chủ chưa trả JSON.");
            }
            const data = await response.json();
            if (!response.ok) throw new Error(data.message || "Không thể gửi bình luận.");

            const item = document.createElement("div");
            item.className = "comment-box rounded p-2 mb-2";
            item.innerHTML = `
                <div class="d-flex justify-content-between align-items-start">
                    <div>
                        <strong>${data.authorFullName}</strong>
                        <small class="text-secondary ms-1">@${data.authorUsername}</small>
                    </div>
                </div>
                <div class="post-content mt-1">${data.content}</div>
                <small class="text-secondary">${data.createdAt || "Vừa xong"}</small>
            `;
            list.appendChild(item);
            input.value = "";
        } catch (error) {
            alert(error.message);
        } finally {
            button.disabled = false;
        }
    }, true);
}());