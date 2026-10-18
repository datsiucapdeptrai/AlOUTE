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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import vn.edu.hcmute.fakebook.dto.CreatePostRequest;
import vn.edu.hcmute.fakebook.enums.ReactionType;
import vn.edu.hcmute.fakebook.service.PostLikeService;
import vn.edu.hcmute.fakebook.service.PostService;

@Controller
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final PostLikeService postLikeService;

    @PostMapping("/posts")
    public String createPost(
            @Valid
            @ModelAttribute("postForm")
            CreatePostRequest postForm,
            BindingResult bindingResult,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute(
                "postError",
                bindingResult.getAllErrors()
                    .get(0)
                    .getDefaultMessage()
            );
            return "redirect:/feed";
        }

        try {
            postService.createPost(
                authentication.getName(),
                postForm
            );
            return "redirect:/feed?created";

        } catch (IllegalArgumentException
                | IllegalStateException exception) {

            redirectAttributes.addFlashAttribute(
                "postError",
                exception.getMessage()
            );
            return "redirect:/feed";
        }
    }

    @PostMapping("/posts/{id}/delete")
    public String deletePost(
            @PathVariable("id") Long id,
            Authentication authentication) {

        postService.deletePost(
            id,
            authentication.getName()
        );
        return "redirect:/feed?deleted";
    }

    // =====================================================
    // REACTION (AJAX)
    // =====================================================
    @PostMapping(value = "/posts/{postId}/reaction",
                 headers = "X-Requested-With=XMLHttpRequest")
    public ResponseEntity<Map<String, Object>> reactAjax(
            @PathVariable Long postId,
            @RequestParam("reactionType") String reactionType,
            Authentication authentication) {

        try {
            ReactionType type = ReactionType.valueOf(
                reactionType.toUpperCase()
            );

            Map<String, Object> result = postLikeService.toggleReaction(
                postId,
                authentication.getName(),
                type
            );

            return ResponseEntity.ok(result);

        } catch (AccessDeniedException exception) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("success", false, "message", exception.getMessage()));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", exception.getMessage()));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", exception.getMessage()));
        }
    }

    // =====================================================
    // REACTION (dự phòng khi không có JS)
    // =====================================================
    @PostMapping("/posts/{postId}/reaction")
    public String react(
            @PathVariable Long postId,
            @RequestParam("reactionType") String reactionType,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        try {
            ReactionType type = ReactionType.valueOf(
                reactionType.toUpperCase()
            );
            postLikeService.toggleReaction(
                postId,
                authentication.getName(),
                type
            );
        } catch (IllegalArgumentException | AccessDeniedException exception) {
            redirectAttributes.addFlashAttribute(
                "postError",
                exception.getMessage()
            );
        }
        return "redirect:/feed";
    }
}