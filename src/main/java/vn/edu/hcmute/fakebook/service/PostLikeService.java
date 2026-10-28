package vn.edu.hcmute.fakebook.service;

import java.util.HashMap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import vn.edu.hcmute.fakebook.entity.Friendship;
import vn.edu.hcmute.fakebook.entity.Post;
import vn.edu.hcmute.fakebook.entity.PostLike;
import vn.edu.hcmute.fakebook.entity.UserAccount;
import vn.edu.hcmute.fakebook.enums.FriendshipStatus;
import vn.edu.hcmute.fakebook.enums.NotificationType;
import vn.edu.hcmute.fakebook.enums.PostStatus;
import vn.edu.hcmute.fakebook.enums.PostVisibility;
import vn.edu.hcmute.fakebook.enums.ReactionType;
import vn.edu.hcmute.fakebook.repository.FriendshipRepository;
import vn.edu.hcmute.fakebook.repository.PostLikeRepository;
import vn.edu.hcmute.fakebook.repository.PostRepository;
import vn.edu.hcmute.fakebook.repository.UserAccountRepository;

@Service
@RequiredArgsConstructor
public class PostLikeService {

    private final PostLikeRepository postLikeRepository;
    private final PostRepository postRepository;
    private final UserAccountRepository userAccountRepository;
    private final FriendshipRepository friendshipRepository;
    private final NotificationService notificationService;

    /**
     * Toggle reaction. Trả về Map chứa thông tin reaction mới.
     */
    @Transactional
    public Map<String, Object> toggleReaction(
            Long postId, String username, ReactionType reactionType) {

        UserAccount currentUser = findUser(username);
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bài viết"));

        checkCanViewPost(currentUser, post);

        Optional<PostLike> existing = postLikeRepository
            .findByPostIdAndUserId(postId, currentUser.getId());

        boolean removed = false;
        ReactionType finalReaction = reactionType;

        if (existing.isPresent()) {
            PostLike like = existing.get();
            if (like.getReactionType() == reactionType) {
                // Bấm cùng reaction → xóa
                postLikeRepository.delete(like);
                removed = true;
                finalReaction = null;
            } else {
                // Đổi reaction
                like.setReactionType(reactionType);
                postLikeRepository.save(like);
            }
        } else {
            // Tạo mới
            PostLike like = new PostLike();
            like.setPost(post);
            like.setUser(currentUser);
            like.setReactionType(reactionType);
            postLikeRepository.save(like);

            // Thông báo
            notificationService.sendNotification(
                currentUser,
                post.getAuthor(),
                NotificationType.POST_LIKED,
                "đã thả cảm xúc " + reactionType.getEmoji() + " vào bài viết của bạn.",
                post.getId(),
                "/feed#post-" + post.getId()
            );
        }

        long totalCount = postLikeRepository.countByPostId(postId);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("removed", removed);
        result.put("reactionType", finalReaction == null ? null : finalReaction.name());
        result.put("totalCount", totalCount);
        return result;
    }

    /**
     * Lấy reaction của user cho nhiều bài viết (dùng render feed).
     * Trả về Map<postId, ReactionType>
     */
    @Transactional(readOnly = true)
    public Map<Long, ReactionType> getUserReactions(
            List<Post> posts, String username) {

        if (posts == null || posts.isEmpty()) return Map.of();

        UserAccount currentUser = findUser(username);
        List<Long> postIds = posts.stream().map(Post::getId).toList();

        Map<Long, ReactionType> result = new HashMap<>();
        for (Object[] row : postLikeRepository.findUserReactions(currentUser.getId(), postIds)) {
            Long postId = ((Number) row[0]).longValue();
            ReactionType type = (ReactionType) row[1];
            result.put(postId, type);
        }
        return result;
    }

    /**
     * Thống kê reaction cho nhiều bài viết.
     * Trả về Map<postId, Map<ReactionType, count>>
     */
    @Transactional(readOnly = true)
    public Map<Long, Map<ReactionType, Long>> getReactionStats(List<Post> posts) {
        if (posts == null || posts.isEmpty()) return Map.of();

        List<Long> postIds = posts.stream().map(Post::getId).toList();
        Map<Long, Map<ReactionType, Long>> result = new LinkedHashMap<>();

        for (Object[] row : postLikeRepository.countReactionsByPostIds(postIds)) {
            Long postId = ((Number) row[0]).longValue();
            ReactionType type = (ReactionType) row[1];
            Long count = ((Number) row[2]).longValue();

            result.computeIfAbsent(postId, k -> new LinkedHashMap<>())
                  .put(type, count);
        }
        return result;
    }

    // ===== Các method cũ giữ nguyên =====

    @Transactional(readOnly = true)
    public Map<Long, Long> getLikeCounts(List<Post> posts) {
        if (posts == null || posts.isEmpty()) return Map.of();

        List<Long> postIds = posts.stream().map(Post::getId).toList();
        Map<Long, Long> result = new LinkedHashMap<>();
        postIds.forEach(id -> result.put(id, 0L));

        postLikeRepository.countLikesByPostIds(postIds).forEach(row -> {
            Long postId = ((Number) row[0]).longValue();
            Long count = ((Number) row[1]).longValue();
            result.put(postId, count);
        });
        return result;
    }

    @Transactional(readOnly = true)
    public Set<Long> getLikedPostIds(List<Post> posts, String username) {
        if (posts == null || posts.isEmpty()) return Set.of();
        UserAccount user = findUser(username);
        List<Long> postIds = posts.stream().map(Post::getId).toList();
        return postLikeRepository.findLikedPostIds(user.getId(), postIds);
    }

    private void checkCanViewPost(UserAccount currentUser, Post post) {
        if (post.getStatus() != PostStatus.ACTIVE) {
            throw new IllegalArgumentException("Bài viết không còn tồn tại");
        }
        if (post.getAuthor().getId().equals(currentUser.getId())) return;
        if (post.getVisibility() == PostVisibility.PUBLIC) return;
        if (post.getVisibility() == PostVisibility.PRIVATE) {
            throw new AccessDeniedException("Bạn không có quyền thả cảm xúc");
        }

        Long one = Math.min(currentUser.getId(), post.getAuthor().getId());
        Long two = Math.max(currentUser.getId(), post.getAuthor().getId());
        Friendship f = friendshipRepository.findByUserOneIdAndUserTwoId(one, two)
            .orElseThrow(() -> new AccessDeniedException("Bạn không có quyền thả cảm xúc"));
        if (f.getStatus() != FriendshipStatus.ACCEPTED) {
            throw new AccessDeniedException("Bạn không có quyền thả cảm xúc");
        }
    }

    private UserAccount findUser(String username) {
        return userAccountRepository.findByUsernameIgnoreCase(username)
            .orElseThrow(() -> new IllegalStateException("Không tìm thấy người dùng"));
    }
    @Transactional
    public boolean toggleLike(Long postId, String username) {
        UserAccount user = userAccountRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài viết"));

        // Tìm xem user này đã like bài này chưa
        Optional<PostLike> existingLike = postLikeRepository.findByPostIdAndUserId(postId, user.getId());

        if (existingLike.isPresent()) {
            // Nếu có rồi -> Xóa (Unlike)
            postLikeRepository.delete(existingLike.get());
            return false;
        } else {
            // Nếu chưa có -> Thêm mới
            PostLike newLike = new PostLike();
            newLike.setPost(post);
            newLike.setUser(user); // Nếu Entity của bạn tên là setAuthor thì đổi lại nhé
            postLikeRepository.save(newLike);
            return true;
        }
    }

    public long countLikes(Long postId) {
        return postLikeRepository.countByPostId(postId);
    }
}