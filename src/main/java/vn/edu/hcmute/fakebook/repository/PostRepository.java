package vn.edu.hcmute.fakebook.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import vn.edu.hcmute.fakebook.entity.Post;
import vn.edu.hcmute.fakebook.enums.FriendshipStatus;
import vn.edu.hcmute.fakebook.enums.PostStatus;
import vn.edu.hcmute.fakebook.enums.PostVisibility;

public interface PostRepository extends JpaRepository<Post, Long> {

    // =====================================================
    // FEED - LẤY BÀI VIẾT HIỂN THỊ (PUBLIC + FRIENDS + CỦA MÌNH)
    // =====================================================
    @EntityGraph(attributePaths = {"author", "videoCategory"})
    @Query("""
        SELECT DISTINCT p FROM Post p
        LEFT JOIN Friendship f
            ON (f.userOne.id = :currentUserId AND f.userTwo.id = p.author.id)
            OR (f.userTwo.id = :currentUserId AND f.userOne.id = p.author.id)
        WHERE p.status = :status
          AND (
              p.author.id = :currentUserId
              OR p.visibility = :publicVisibility
              OR (p.visibility = :friendsVisibility
                  AND f.status = :friendshipStatus)
          )
        ORDER BY p.createdAt DESC
        """)
    Page<Post> findVisiblePosts(
        @Param("currentUserId") Long currentUserId,
        @Param("status") PostStatus status,
        @Param("publicVisibility") PostVisibility publicVisibility,
        @Param("friendsVisibility") PostVisibility friendsVisibility,
        @Param("friendshipStatus") FriendshipStatus friendshipStatus,
        Pageable pageable
    );	
 // =====================================================
 // SEARCH FEED - SẮP XẾP THEO MỚI NHẤT
 // =====================================================
 @EntityGraph(attributePaths = {"author", "videoCategory"})
 @Query("""
     SELECT DISTINCT p FROM Post p
     LEFT JOIN Friendship f
         ON (f.userOne.id = :currentUserId AND f.userTwo.id = p.author.id)
         OR (f.userTwo.id = :currentUserId AND f.userOne.id = p.author.id)
     WHERE p.status = :status
       AND (:keyword = ''
            OR LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(p.author.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(p.author.username) LIKE LOWER(CONCAT('%', :keyword, '%')))
       AND (:type = 'ALL'
            OR (:type = 'TEXT'
                AND (p.imageUrl IS NULL OR p.imageUrl = '')
                AND (p.videoUrl IS NULL OR p.videoUrl = ''))
            OR (:type = 'IMAGE'
                AND p.imageUrl IS NOT NULL AND p.imageUrl <> '')
            OR (:type = 'VIDEO'
                AND p.videoUrl IS NOT NULL AND p.videoUrl <> ''))
       AND (
           p.author.id = :currentUserId
           OR p.visibility = :publicVisibility
           OR (p.visibility = :friendsVisibility
               AND f.status = :friendshipStatus)
       )
     ORDER BY p.createdAt DESC
     """)
 Page<Post> searchFeedPostsByNewest(
     @Param("currentUserId") Long currentUserId,
     @Param("keyword") String keyword,
     @Param("type") String type,
     @Param("status") PostStatus status,
     @Param("publicVisibility") PostVisibility publicVisibility,
     @Param("friendsVisibility") PostVisibility friendsVisibility,
     @Param("friendshipStatus") FriendshipStatus friendshipStatus,
     Pageable pageable
 );

 // =====================================================
 // SEARCH FEED - SẮP XẾP THEO NHIỀU LƯỢT XEM NHẤT
 // =====================================================
 @EntityGraph(attributePaths = {"author", "videoCategory"})
 @Query("""
     SELECT DISTINCT p FROM Post p
     LEFT JOIN Friendship f
         ON (f.userOne.id = :currentUserId AND f.userTwo.id = p.author.id)
         OR (f.userTwo.id = :currentUserId AND f.userOne.id = p.author.id)
     WHERE p.status = :status
       AND (:keyword = ''
            OR LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(p.author.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(p.author.username) LIKE LOWER(CONCAT('%', :keyword, '%')))
       AND (:type = 'ALL'
            OR (:type = 'TEXT'
                AND (p.imageUrl IS NULL OR p.imageUrl = '')
                AND (p.videoUrl IS NULL OR p.videoUrl = ''))
            OR (:type = 'IMAGE'
                AND p.imageUrl IS NOT NULL AND p.imageUrl <> '')
            OR (:type = 'VIDEO'
                AND p.videoUrl IS NOT NULL AND p.videoUrl <> ''))
       AND (
           p.author.id = :currentUserId
           OR p.visibility = :publicVisibility
           OR (p.visibility = :friendsVisibility
               AND f.status = :friendshipStatus)
       )
     ORDER BY p.viewCount DESC, p.createdAt DESC
     """)
 Page<Post> searchFeedPostsByPopular(
     @Param("currentUserId") Long currentUserId,
     @Param("keyword") String keyword,
     @Param("type") String type,
     @Param("status") PostStatus status,
     @Param("publicVisibility") PostVisibility publicVisibility,
     @Param("friendsVisibility") PostVisibility friendsVisibility,
     @Param("friendshipStatus") FriendshipStatus friendshipStatus,
     Pageable pageable
 );

    // =====================================================
    // VIDEO - LẤY VIDEO HIỂN THỊ THEO CATEGORY
    // =====================================================
    @EntityGraph(attributePaths = {"author", "videoCategory"})
    @Query("""
        SELECT DISTINCT p FROM Post p
        LEFT JOIN Friendship f
            ON (f.userOne.id = :currentUserId AND f.userTwo.id = p.author.id)
            OR (f.userTwo.id = :currentUserId AND f.userOne.id = p.author.id)
        WHERE p.status = :status
          AND p.videoUrl IS NOT NULL
          AND p.videoUrl <> ''
          AND (:categoryId IS NULL OR p.videoCategory.id = :categoryId)
          AND (
              p.author.id = :currentUserId
              OR p.visibility = :publicVisibility
              OR (p.visibility = :friendsVisibility
                  AND f.status = :friendshipStatus)
          )
        ORDER BY p.createdAt DESC
        """)
    Page<Post> findVisibleVideoPosts(
        @Param("currentUserId") Long currentUserId,
        @Param("categoryId") Long categoryId,
        @Param("status") PostStatus status,
        @Param("publicVisibility") PostVisibility publicVisibility,
        @Param("friendsVisibility") PostVisibility friendsVisibility,
        @Param("friendshipStatus") FriendshipStatus friendshipStatus,
        Pageable pageable
    );

    // =====================================================
    // CHI TIẾT BÀI VIẾT (fetch author + videoCategory để tránh lazy)
    // =====================================================
    @EntityGraph(attributePaths = {"author", "videoCategory"})
    @Query("SELECT p FROM Post p WHERE p.id = :postId")
    Optional<Post> findPostDetailsById(@Param("postId") Long postId);

    // =====================================================
    // TĂNG VIEW / SHARE COUNT
    // =====================================================
    @Modifying
    @Query("UPDATE Post p SET p.viewCount = p.viewCount + 1 WHERE p.id = :postId")
    void incrementViewCount(@Param("postId") Long postId);

    @Modifying
    @Query("UPDATE Post p SET p.shareCount = p.shareCount + 1 WHERE p.id = :postId")
    void incrementShareCount(@Param("postId") Long postId);

    // =====================================================
    // ĐẾM VIDEO THEO CATEGORY (dùng cho sidebar video)
    // =====================================================
    @Query("""
        SELECT COUNT(p)
        FROM Post p
        WHERE p.videoCategory.id = :categoryId
          AND p.videoUrl IS NOT NULL
          AND p.videoUrl <> ''
          AND p.status = :status
        """)
    long countByVideoCategoryIdAndVideoUrlIsNotNullAndStatus(
        @Param("categoryId") Long categoryId,
        @Param("status") PostStatus status
    );

    // =====================================================
    // PROFILE USER
    // =====================================================
    long countByAuthorId(Long authorId);

    @EntityGraph(attributePaths = {"author", "videoCategory"})
    Page<Post> findByAuthorIdOrderByCreatedAtDesc(
        Long authorId,
        Pageable pageable
    );

    // =====================================================
    // ADMIN - QUẢN LÝ BÀI VIẾT
    // =====================================================
    @EntityGraph(attributePaths = {"author", "videoCategory"})
    @Query("""
        SELECT p FROM Post p
        WHERE (:keyword = ''
            OR LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%')))
        ORDER BY p.createdAt DESC
        """)
    Page<Post> searchForAdmin(
        @Param("keyword") String keyword,
        Pageable pageable
    );

    long countByStatus(PostStatus status);
    long countByVideoUrlIsNotNullAndStatus(PostStatus status);

    @Query("SELECT COALESCE(SUM(p.viewCount), 0) FROM Post p WHERE p.status = :status")
    long sumViewCountByStatus(@Param("status") PostStatus status);

    @Query("SELECT COALESCE(SUM(p.shareCount), 0) FROM Post p WHERE p.status = :status")
    long sumShareCountByStatus(@Param("status") PostStatus status);

    // =====================================================
    // ADMIN - QUẢN LÝ VIDEO
    // =====================================================
    @EntityGraph(attributePaths = {"author", "videoCategory"})
    Page<Post> findByVideoUrlIsNotNullAndStatus(
        PostStatus status,
        Pageable pageable
    );
}