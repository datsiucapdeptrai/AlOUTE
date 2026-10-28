package vn.edu.hcmute.fakebook.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import vn.edu.hcmute.fakebook.entity.PostLike;
import vn.edu.hcmute.fakebook.enums.ReactionType;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    Optional<PostLike> findByPostIdAndUserId(Long postId, Long userId);

    @Query("""
        SELECT pl.post.id, COUNT(pl.id)
        FROM PostLike pl
        WHERE pl.post.id IN :postIds
        GROUP BY pl.post.id
        """)
    List<Object[]> countLikesByPostIds(@Param("postIds") Collection<Long> postIds);

    @Query("""
        SELECT pl.post.id
        FROM PostLike pl
        WHERE pl.user.id = :userId
          AND pl.post.id IN :postIds
        """)
    Set<Long> findLikedPostIds(
        @Param("userId") Long userId,
        @Param("postIds") Collection<Long> postIds
    );

    // ✅ Lấy reaction của user cho nhiều bài viết
    @Query("""
    	    SELECT pl.post.id, pl.reactionType
    	    FROM PostLike pl
    	    WHERE pl.user.id = :userId
    	      AND pl.post.id IN :postIds
    	      AND pl.reactionType IS NOT NULL
    	    """)
    	List<Object[]> findUserReactions(
    	    @Param("userId") Long userId,
    	    @Param("postIds") Collection<Long> postIds
    	);

    // ✅ Thống kê số lượng từng loại reaction cho 1 bài viết
    @Query("""
        SELECT pl.reactionType, COUNT(pl.id)
        FROM PostLike pl
        WHERE pl.post.id = :postId
        GROUP BY pl.reactionType
        ORDER BY COUNT(pl.id) DESC
        """)
    List<Object[]> countReactionsByPostId(@Param("postId") Long postId);

    // ✅ Thống kê reaction cho nhiều bài viết (dùng cho feed)
    @Query("""
    	    SELECT pl.post.id, pl.reactionType, COUNT(pl.id)
    	    FROM PostLike pl
    	    WHERE pl.post.id IN :postIds
    	      AND pl.reactionType IS NOT NULL
    	    GROUP BY pl.post.id, pl.reactionType
    	    ORDER BY pl.post.id, COUNT(pl.id) DESC
    	    """)
    	List<Object[]> countReactionsByPostIds(@Param("postIds") Collection<Long> postIds);

    long countByPostId(Long postId);

    // ✅ METHOD MỚI - ĐẾM TỔNG LIKE CỦA 1 TÁC GIẢ (cho Profile)
    @Query("""
        SELECT COUNT(pl.id)
        FROM PostLike pl
        WHERE pl.post.author.id = :authorId
          AND pl.post.status = vn.edu.hcmute.fakebook.enums.PostStatus.ACTIVE
        """)
    long countLikesByAuthorId(@Param("authorId") Long authorId);
}