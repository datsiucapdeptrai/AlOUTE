package vn.edu.hcmute.fakebook.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.hcmute.fakebook.entity.PostView;

public interface PostViewRepository extends JpaRepository<PostView, Long> {
    boolean existsByPostIdAndViewerId(Long postId, Long viewerId);
}