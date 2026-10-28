(function () {
    "use strict";

    // ============ REACTION (LIKE) ============
    document.addEventListener("click", async function (event) {
        const btn = event.target.closest(".reaction-btn, .reaction-main-btn");
        if (!btn) return;

        const wrapper = btn.closest(".reaction-wrapper");
        if (!wrapper) return;

        const mainBtn = wrapper.querySelector(".reaction-main-btn");
        const postId = mainBtn.dataset.postId;
        
        // Xác định loại cảm xúc được chọn
        let reactionType = "LIKE";
        if (btn.classList.contains("reaction-btn")) {
            reactionType = btn.dataset.reaction; 
        } else {
            const currentReaction = mainBtn.dataset.userReaction;
            reactionType = currentReaction ? currentReaction : "LIKE"; 
        }

        try {
            // Gọi API kèm theo reactionType
            const response = await fetch(`/api/posts/${postId}/like?reactionType=${reactionType}`, {
                method: "POST",
                headers: { "Content-Type": "application/json" }
            });

            if (response.status === 401 || response.status === 403) {
                alert("Vui lòng đăng nhập để thả cảm xúc.");
                return;
            }

            const data = await response.json();
            if (data.error) throw new Error(data.error);

            const iconSpan = mainBtn.querySelector(".reaction-icon");
            const labelSpan = mainBtn.querySelector(".reaction-label");
            const countSpan = document.getElementById(`like-count-${postId}`);

            if (!data.liked) {
                // Trường hợp bấm lần 2 để bỏ Like (Unlike)
                mainBtn.dataset.userReaction = "";
                mainBtn.classList.remove("btn-primary", "btn-danger", "btn-warning");
                mainBtn.classList.add("btn-outline-primary");
                iconSpan.textContent = "👍";
                labelSpan.textContent = "Thích";
            } else {
                // Trường hợp Like / Thả cảm xúc
                const map = {
                    LIKE:  { icon: "👍", label: "Thích",     cls: "btn-primary" },
                    LOVE:  { icon: "❤️", label: "Yêu thích", cls: "btn-danger"  },
                    HAHA:  { icon: "😆", label: "Haha",      cls: "btn-warning" },
                    WOW:   { icon: "😮", label: "Wow",       cls: "btn-warning" },
                    SAD:   { icon: "😢", label: "Buồn",      cls: "btn-warning" },
                    ANGRY: { icon: "😡", label: "Phẫn nộ",   cls: "btn-danger"  }
                };
                const info = map[reactionType] || map.LIKE;
                
                mainBtn.dataset.userReaction = reactionType;
                mainBtn.classList.remove("btn-outline-primary", "btn-primary", "btn-danger", "btn-warning");
                mainBtn.classList.add(info.cls);
                iconSpan.textContent = info.icon;
                labelSpan.textContent = info.label;
            }

            if (countSpan) countSpan.textContent = data.likeCount;

        } catch (error) {
            alert(error.message || "Không thể thả cảm xúc.");
        }
    });

    // ============ COMMENT (Giữ nguyên) ============
    document.addEventListener("submit", async function (event) {
        const form = event.target;
        if (!(form instanceof HTMLFormElement) || !form.classList.contains("comment-form")) return;

        event.preventDefault();
        const button = form.querySelector('button[type="submit"]');
        const input = form.querySelector('input[name="content"]');
        const postId = form.dataset.postId;
        const list = document.getElementById("comments-post-" + postId);
        
        if (!input.value.trim()) return;
        button.disabled = true;

        try {
            const response = await fetch(`/api/posts/${postId}/comments`, {
                method: "POST",
                headers: { 
                    "Content-Type": "application/json",
                    "Accept": "application/json" 
                },
                body: JSON.stringify({ content: input.value })
            });

            const data = await response.json();
            if (!response.ok) throw new Error(data.error || "Không thể gửi bình luận.");

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