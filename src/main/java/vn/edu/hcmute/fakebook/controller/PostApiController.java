package vn.edu.hcmute.fakebook.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import vn.edu.hcmute.fakebook.entity.Comment;
import vn.edu.hcmute.fakebook.entity.Post;
import vn.edu.hcmute.fakebook.enums.ReactionType; // QUAN TRỌNG: Thêm import này
import vn.edu.hcmute.fakebook.service.CommentService;
import vn.edu.hcmute.fakebook.service.PostLikeService;
import vn.edu.hcmute.fakebook.service.PostService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostApiController {

    private final PostLikeService postLikeService;
    private final CommentService commentService;
    private final PostService postService;

    // ================= API LIKE / REACTION =================
    @PostMapping("/{id}/like")
    public ResponseEntity<?> toggleLike(
            @PathVariable("id") Long postId,
            @RequestParam(value = "reactionType", defaultValue = "LIKE") String reactionType, // NHẬN THÊM THAM SỐ NÀY
            @AuthenticationPrincipal UserDetails userDetails) {
        
        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Vui lòng đăng nhập để thích bài viết"));
        }

        try {
            // GỌI HÀM toggleReaction THAY VÌ toggleLike
            Map<String, Object> result = postLikeService.toggleReaction(
                    postId, 
                    userDetails.getUsername(), 
                    ReactionType.valueOf(reactionType)
            );

            // Lấy dữ liệu từ kết quả trả về của Service
            boolean isRemoved = (boolean) result.get("removed");
            Object count = result.get("totalCount");

            return ResponseEntity.ok(Map.of(
                    "liked", !isRemoved, // Nếu bị removed = true thì liked = false
                    "likeCount", count,
                    "reactionType", reactionType
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ================= API COMMENT =================
    public record CommentRequest(String content) {}

    @PostMapping("/{id}/comments")
    public ResponseEntity<?> addComment(
            @PathVariable("id") Long postId,
            @RequestBody CommentRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Vui lòng đăng nhập để bình luận"));
        }
        if (request.content() == null || request.content().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Nội dung bình luận không được để trống"));
        }

        try {
            Comment newComment = commentService.addComment(postId, request.content(), userDetails.getUsername());
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", newComment.getId());
            response.put("content", newComment.getContent());
            response.put("authorFullName", newComment.getAuthor().getFullName());
            response.put("authorUsername", newComment.getAuthor().getUsername());
            response.put("authorAvatar", newComment.getAuthor().getProfile() != null && newComment.getAuthor().getProfile().getAvatarUrl() != null 
                            ? newComment.getAuthor().getProfile().getAvatarUrl() 
                            : "/images/default-avatar.png");
            response.put("createdAt", newComment.getCreatedAt() != null ? newComment.getCreatedAt().toString() : "Vừa xong");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ================= API SHARE =================
    public record ShareRequest(String caption) {}

    @PostMapping("/{id}/share")
    public ResponseEntity<?> sharePost(
            @PathVariable("id") Long postId,
            @RequestBody ShareRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Vui lòng đăng nhập để chia sẻ"));
        }

        try {
            Post sharedPost = postService.sharePost(postId, userDetails.getUsername(), request.caption());
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đã chia sẻ bài viết lên trang cá nhân của bạn!",
                    "newPostId", sharedPost.getId()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}