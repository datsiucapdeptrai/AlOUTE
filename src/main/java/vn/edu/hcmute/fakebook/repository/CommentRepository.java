package vn.edu.hcmute.fakebook.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import vn.edu.hcmute.fakebook.entity.Comment;
import vn.edu.hcmute.fakebook.enums.CommentStatus;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @EntityGraph(attributePaths = "author")
    List<Comment> findByPostIdAndStatusOrderByCreatedAtAsc(
        Long postId, CommentStatus status
    );

    @EntityGraph(attributePaths = "author")
    @Query("""
        SELECT c FROM Comment c
        WHERE c.post.id IN :postIds
          AND c.status = :status
        ORDER BY c.createdAt ASC
        """)
    List<Comment> findByPostIdsAndStatus(
        @Param("postIds") Collection<Long> postIds,
        @Param("status") CommentStatus status
    );

    @EntityGraph(attributePaths = "author")
    @Query("""
        SELECT c FROM Comment c
        WHERE (:keyword = '' 
            OR LOWER(c.content) LIKE LOWER(CONCAT('%', :keyword, '%')))
        ORDER BY c.createdAt DESC
        """)
    Page<Comment> searchForAdmin(
        @Param("keyword") String keyword,
        Pageable pageable
    );

    long countByStatus(CommentStatus status);
}