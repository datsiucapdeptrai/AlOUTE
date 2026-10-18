package vn.edu.hcmute.fakebook.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import lombok.RequiredArgsConstructor;
import vn.edu.hcmute.fakebook.dto.CreateCommentRequest;
import vn.edu.hcmute.fakebook.entity.Post;
import vn.edu.hcmute.fakebook.entity.UserAccount;
import vn.edu.hcmute.fakebook.entity.VideoCategory;
import vn.edu.hcmute.fakebook.enums.PostStatus;
import vn.edu.hcmute.fakebook.repository.PostRepository;
import vn.edu.hcmute.fakebook.repository.UserAccountRepository;
import vn.edu.hcmute.fakebook.repository.VideoCategoryRepository;
import vn.edu.hcmute.fakebook.service.CommentService;
import vn.edu.hcmute.fakebook.service.PostLikeService;
import vn.edu.hcmute.fakebook.service.PostService;
import vn.edu.hcmute.fakebook.service.VideoInteractionService;

@Controller
@RequiredArgsConstructor
public class VideoController {

    private final PostService postService;
    private final VideoInteractionService interactionService;
    private final VideoCategoryRepository categoryRepository;
    private final PostRepository postRepository;
    private final UserAccountRepository userAccountRepository;
    private final CommentService commentService;
    private final PostLikeService postLikeService;

    @GetMapping("/videos")
    public String listVideos(
            @RequestParam(required = false) Long category,
            @RequestParam(defaultValue = "0") int page,
            Authentication authentication,
            Model model) {

        Page<Post> videos = postService.getVideoPosts(
            authentication.getName(), category, page
        );

        List<VideoCategory> categories =
            categoryRepository.findByActiveTrueOrderByNameAsc();

        Map<Long, Long> videoCounts = new LinkedHashMap<>();
        for (VideoCategory item : categories) {
            // FIX: Bỏ dấu ) thừa
            long count = postRepository
                .countByVideoCategoryIdAndVideoUrlIsNotNullAndStatus(
                    item.getId(), PostStatus.ACTIVE
                );
            videoCounts.put(item.getId(), count);
        }

        model.addAttribute("currentUser", currentUser(authentication));
        model.addAttribute("videos", videos);
        model.addAttribute("categories", categories);
        model.addAttribute("videoCounts", videoCounts);
        model.addAttribute("selectedCategoryId", category);
        model.addAttribute(
            "likeCounts",
            postLikeService.getLikeCounts(videos.getContent())
        );
        return "videos/index";
    }

    @GetMapping("/videos/{postId}")
    public String videoDetail(
            @PathVariable Long postId,
            Authentication authentication,
            Model model) {

        Post post = postService.getVisiblePost(postId, authentication.getName());
        if (post.getVideoUrl() == null || post.getVideoUrl().isBlank()) {
            throw new IllegalArgumentException("Bài viết này không có video");
        }

        List<Post> selected = List.of(post);
        model.addAttribute("currentUser", currentUser(authentication));
        model.addAttribute("post", post);
        model.addAttribute("comments", commentService
            .getCommentsForPosts(selected).getOrDefault(postId, List.of()));
        model.addAttribute("commentForm", new CreateCommentRequest());
        model.addAttribute("likeCount", postLikeService
            .getLikeCounts(selected).getOrDefault(postId, 0L));
        model.addAttribute("liked", postLikeService
            .getLikedPostIds(selected, authentication.getName()).contains(postId));
        return "videos/detail";
    }

    @PostMapping("/videos/{postId}/view")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> recordView(
            @PathVariable Long postId,
            Authentication authentication) {
        return interact(() -> Map.of(
            "success", true,
            "viewCount", interactionService.recordView(postId, authentication.getName())
        ));
    }

    @PostMapping("/videos/{postId}/share")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> recordShare(
            @PathVariable Long postId,
            Authentication authentication) {
        return interact(() -> Map.of(
            "success", true,
            "shareCount", interactionService.recordShare(postId, authentication.getName())
        ));
    }

    private UserAccount currentUser(Authentication authentication) {
        return userAccountRepository
            .findByUsernameIgnoreCase(authentication.getName())
            .orElseThrow(() -> new IllegalStateException("Không tìm thấy người dùng"));
    }

    private ResponseEntity<Map<String, Object>> interact(
            java.util.function.Supplier<Map<String, Object>> action) {
        try {
            return ResponseEntity.ok(action.get());
        } catch (AccessDeniedException exception) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("success", false, "message", exception.getMessage()));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", exception.getMessage()));
        }
    }
}