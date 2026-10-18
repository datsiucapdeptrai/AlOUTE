package vn.edu.hcmute.fakebook.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import vn.edu.hcmute.fakebook.dto.CreateCommentRequest;
import vn.edu.hcmute.fakebook.entity.Comment;
import vn.edu.hcmute.fakebook.service.CommentService;

@Controller
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    // Khi JavaScript gửi bằng fetch(), trả JSON để cập nhật bài viết tại chỗ.
    @PostMapping(value = "/posts/{postId}/comments",
                 headers = "X-Requested-With=XMLHttpRequest")
    public ResponseEntity<Map<String, Object>> createCommentAjax(
            @PathVariable Long postId,
            @Valid @ModelAttribute("commentForm") CreateCommentRequest commentForm,
            BindingResult bindingResult,
            Authentication authentication) {

        if (bindingResult.hasErrors()) {
            String message = bindingResult.getAllErrors().get(0).getDefaultMessage();
            return ResponseEntity.badRequest()
                .body(errorResponse(message == null ? "Bình luận không hợp lệ." : message));
        }

        try {
            Comment saved = commentService.createComment(
                postId, authentication.getName(), commentForm.getContent()
            );
            return ResponseEntity.ok(Map.of(
                "success", true,
                "id", saved.getId(),
                "content", saved.getContent(),
                "authorFullName", saved.getAuthor().getFullName() == null
                    ? saved.getAuthor().getUsername()
                    : saved.getAuthor().getFullName(),
                "authorUsername", saved.getAuthor().getUsername(),
                "createdAt", saved.getCreatedAt() == null
                    ? "" : saved.getCreatedAt().toString()
            ));
        } catch (AccessDeniedException exception) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(errorResponse(exception.getMessage()));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest()
                .body(errorResponse(exception.getMessage()));
        }
    }

    // Giữ cách gửi form cũ khi trình duyệt không chạy JavaScript.
    @PostMapping("/posts/{postId}/comments")
    public String createComment(
            @PathVariable Long postId,
            @Valid @ModelAttribute("commentForm") CreateCommentRequest commentForm,
            BindingResult bindingResult,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute(
                "commentError",
                bindingResult.getAllErrors().get(0).getDefaultMessage()
            );
            redirectAttributes.addFlashAttribute("commentPostId", postId);
            return "redirect:/feed";
        }

        try {
            commentService.createComment(
                postId, authentication.getName(), commentForm.getContent()
            );
            redirectAttributes.addFlashAttribute(
                "commentMessage", "Đã thêm bình luận."
            );
        } catch (IllegalArgumentException | AccessDeniedException exception) {
            redirectAttributes.addFlashAttribute("commentError", exception.getMessage());
            redirectAttributes.addFlashAttribute("commentPostId", postId);
        }

        return "redirect:/feed";
    }

    @PostMapping("/comments/{commentId}/delete")
    public String deleteComment(
            @PathVariable Long commentId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        try {
            commentService.deleteComment(commentId, authentication.getName());
            redirectAttributes.addFlashAttribute(
                "commentMessage", "Đã xóa bình luận."
            );
        } catch (IllegalArgumentException | AccessDeniedException exception) {
            redirectAttributes.addFlashAttribute("commentError", exception.getMessage());
        }

        return "redirect:/feed";
    }

    private Map<String, Object> errorResponse(String message) {
        return Map.of("success", false,
                      "message", message == null ? "Không thể bình luận." : message);
    }
}
