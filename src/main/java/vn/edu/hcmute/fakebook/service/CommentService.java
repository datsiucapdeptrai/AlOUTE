package vn.edu.hcmute.fakebook.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import vn.edu.hcmute.fakebook.entity.Comment;
import vn.edu.hcmute.fakebook.entity.Friendship;
import vn.edu.hcmute.fakebook.entity.Post;
import vn.edu.hcmute.fakebook.entity.UserAccount;
import vn.edu.hcmute.fakebook.enums.CommentStatus;
import vn.edu.hcmute.fakebook.enums.FriendshipStatus;
import vn.edu.hcmute.fakebook.enums.NotificationType;
import vn.edu.hcmute.fakebook.enums.PostStatus;
import vn.edu.hcmute.fakebook.enums.PostVisibility;
import vn.edu.hcmute.fakebook.repository.CommentRepository;
import vn.edu.hcmute.fakebook.repository.FriendshipRepository;
import vn.edu.hcmute.fakebook.repository.PostRepository;
import vn.edu.hcmute.fakebook.repository.UserAccountRepository;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserAccountRepository userAccountRepository;
    private final FriendshipRepository friendshipRepository;
    private final NotificationService notificationService;

    // ==============================================================
    // HÀM MỚI THÊM: Để PostApiController gọi được (khớp tham số)
    // ==============================================================
    @Transactional
    public Comment addComment(Long postId, String content, String username) {
        // Gọi lại hàm createComment có sẵn của bạn, chỉ đổi vị trí tham số
        return createComment(postId, username, content);
    }

    // Hàm gốc của bạn (Giữ nguyên)
    @Transactional
    public Comment createComment(Long postId, String username, String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Nội dung bình luận không được để trống"
            );
        }

        Post post = postRepository.findById(postId)
            .orElseThrow(() ->
                new IllegalArgumentException("Không tìm thấy bài viết")
            );
        UserAccount author = findUser(username);

        // Phương thức này đã có sẵn nhưng trước đó chưa được gọi.
        checkCanViewPost(author, post);

        Comment comment = new Comment();
        comment.setPost(post);
        comment.setAuthor(author);
        comment.setContent(content.trim());
        comment.setStatus(CommentStatus.ACTIVE);

        Comment savedComment = commentRepository.saveAndFlush(comment);

        notificationService.sendNotification(
            author,
            post.getAuthor(),
            NotificationType.POST_COMMENTED,
            "đã bình luận bài viết của bạn.",
            post.getId(),
            "/feed#post-" + post.getId()
        );

        return savedComment;
    }

    @Transactional(readOnly = true)
    public Map<Long, List<Comment>> getCommentsForPosts(List<Post> posts) {
        if (posts == null || posts.isEmpty()) {
            return Map.of();
        }

        List<Long> postIds = posts.stream().map(Post::getId).toList();
        return commentRepository
            .findByPostIdsAndStatus(postIds, CommentStatus.ACTIVE)
            .stream()
            .collect(Collectors.groupingBy(
                comment -> comment.getPost().getId(),
                LinkedHashMap::new,
                Collectors.toList()
            ));
    }

    @Transactional
    public void deleteComment(Long commentId, String username) {
        UserAccount currentUser = findUser(username);
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() ->
                new IllegalArgumentException("Không tìm thấy bình luận")
            );

        if (!comment.getAuthor().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException(
                "Bạn không có quyền xóa bình luận này"
            );
        }
        if (comment.getStatus() != CommentStatus.ACTIVE) {
            throw new IllegalArgumentException("Bình luận không còn tồn tại");
        }

        comment.setStatus(CommentStatus.DELETED);
        commentRepository.save(comment);
    }

    private void checkCanViewPost(UserAccount currentUser, Post post) {
        if (post.getStatus() != PostStatus.ACTIVE) {
            throw new IllegalArgumentException("Bài viết không còn tồn tại");
        }
        if (post.getAuthor().getId().equals(currentUser.getId())) {
            return;
        }
        if (post.getVisibility() == PostVisibility.PUBLIC) {
            return;
        }
        if (post.getVisibility() == PostVisibility.PRIVATE) {
            throw new AccessDeniedException(
                "Bạn không có quyền bình luận bài viết này"
            );
        }

        Long userOneId = Math.min(
            currentUser.getId(), post.getAuthor().getId()
        );
        Long userTwoId = Math.max(
            currentUser.getId(), post.getAuthor().getId()
        );
        Friendship friendship = friendshipRepository
            .findByUserOneIdAndUserTwoId(userOneId, userTwoId)
            .orElseThrow(() ->
                new AccessDeniedException("Chỉ bạn bè mới được bình luận")
            );
        if (friendship.getStatus() != FriendshipStatus.ACCEPTED) {
            throw new AccessDeniedException("Chỉ bạn bè mới được bình luận");
        }
    }

    private UserAccount findUser(String username) {
        return userAccountRepository.findByUsernameIgnoreCase(username)
            .orElseThrow(() ->
                new IllegalStateException("Không tìm thấy người dùng")
            );
    }
}