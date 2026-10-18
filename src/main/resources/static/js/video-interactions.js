(function () {
    "use strict";

    function csrfHeaders() {
        const token = document.querySelector('meta[name="_csrf"]')?.content;
        const header = document.querySelector('meta[name="_csrf_header"]')?.content;
        return token && header ? { [header]: token } : {};
    }

    async function postJson(url) {
        const response = await fetch(url, {
            method: "POST",
            credentials: "same-origin",
            headers: { "Accept": "application/json", ...csrfHeaders() }
        });
        const data = await response.json();
        if (!response.ok) throw new Error(data.message || "Không thể thực hiện thao tác.");
        return data;
    }

    document.querySelectorAll(".tracked-video").forEach(function (video) {
        video.addEventListener("play", async function () {
            if (video.dataset.viewRecorded === "true") return;
            video.dataset.viewRecorded = "true";
            try {
                const data = await postJson(video.dataset.viewUrl);
                const counter = document.getElementById("view-count-" + video.dataset.postId);
                if (counter) counter.textContent = data.viewCount;
            } catch (error) {
                video.dataset.viewRecorded = "false";
                console.error(error);
            }
        }, { once: false });
    });

    document.addEventListener("click", async function (event) {
        const button = event.target.closest(".share-video-button");
        if (!button) return;
        button.disabled = true;
        try {
            const absoluteUrl = new URL(button.dataset.shareUrl, window.location.origin).href;
            if (navigator.share) {
                await navigator.share({
                    title: button.dataset.shareTitle || "Video trên FAKEBOOK",
                    url: absoluteUrl
                });
            } else if (navigator.clipboard) {
                await navigator.clipboard.writeText(absoluteUrl);
                alert("Đã sao chép liên kết video.");
            } else {
                window.prompt("Sao chép liên kết video:", absoluteUrl);
            }
            const data = await postJson(button.dataset.shareEndpoint);
            const counter = document.getElementById("share-count-" + button.dataset.postId);
            if (counter) counter.textContent = data.shareCount;
        } catch (error) {
            if (error && error.name !== "AbortError") {
                alert(error.message || "Không thể chia sẻ video.");
            }
        } finally {
            button.disabled = false;
        }
    });
}());
