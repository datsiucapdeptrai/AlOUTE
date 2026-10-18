package vn.edu.hcmute.fakebook.controller;

import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.RequiredArgsConstructor;
import vn.edu.hcmute.fakebook.dto.CreatePostRequest;
import vn.edu.hcmute.fakebook.entity.Post;
import vn.edu.hcmute.fakebook.entity.UserAccount;
import vn.edu.hcmute.fakebook.repository.UserAccountRepository;
import vn.edu.hcmute.fakebook.service.PostService;
import vn.edu.hcmute.fakebook.dto.CreateCommentRequest;
import vn.edu.hcmute.fakebook.service.CommentService;
import vn.edu.hcmute.fakebook.service.PostLikeService;
import vn.edu.hcmute.fakebook.repository.VideoCategoryRepository;
import java.util.List;
import java.util.Map;
import vn.edu.hcmute.fakebook.service.FriendshipService;

@Controller
@RequiredArgsConstructor
public class FeedController {

    private final UserAccountRepository userAccountRepository;
    private final PostService postService;
    private final CommentService commentService;
    private final PostLikeService postLikeService;
    private final VideoCategoryRepository videoCategoryRepository;
    private final FriendshipService friendshipService;

    @GetMapping("/feed")
    public String showFeed(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "keyword", defaultValue = "") String keyword,
            @RequestParam(name = "type", defaultValue = "ALL") String type,
            @RequestParam(name = "sort", defaultValue = "newest") String sort,
            Authentication authentication,
            Model model) {

        UserAccount currentUser = userAccountRepository
            .findByUsernameIgnoreCase(authentication.getName())
            .orElseThrow(() ->
                new IllegalStateException("Không tìm thấy người dùng")
            );

        Page<Post> posts = postService.searchFeedPosts(
            authentication.getName(),
            keyword,
            type,
            sort,
            page
        );

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("posts", posts);
        model.addAttribute("keyword", keyword);
        model.addAttribute("type", type);
        model.addAttribute("sort", sort);
        model.addAttribute("postForm", new CreatePostRequest());

        model.addAttribute(
            "commentsByPost",
            commentService.getCommentsForPosts(posts.getContent())
        );

        model.addAttribute("commentForm", new CreateCommentRequest());

        model.addAttribute(
            "likeCounts",
            postLikeService.getLikeCounts(posts.getContent())
        );

        model.addAttribute(
            "likedPostIds",
            postLikeService.getLikedPostIds(
                posts.getContent(),
                authentication.getName()
            )
        );

        // ✅ 2 DÒNG QUAN TRỌNG NÀY ĐANG THIẾU Ở FILE CỦA BẠN
        model.addAttribute(
            "userReactions",
            postLikeService.getUserReactions(
                posts.getContent(),
                authentication.getName()
            )
        );
        model.addAttribute(
            "reactionStats",
            postLikeService.getReactionStats(posts.getContent())
        );

     // ✅ Gợi ý kết bạn
        List<UserAccount> friendSuggestions = friendshipService
            .getFriendSuggestionUsers(authentication.getName(), 5);

        Map<Long, Long> mutualFriendCounts = friendshipService
            .getFriendSuggestions(authentication.getName(), 5);

        model.addAttribute("friendSuggestions", friendSuggestions);
        model.addAttribute("mutualFriendCounts", mutualFriendCounts);
        model.addAttribute(
            "videoCategories",
            videoCategoryRepository.findByActiveTrueOrderByNameAsc()
        );

        return "feed/index";
    }
}