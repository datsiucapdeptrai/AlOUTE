package vn.edu.hcmute.fakebook.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.hcmute.fakebook.entity.VideoCategory;

public interface VideoCategoryRepository
        extends JpaRepository<VideoCategory, Long> {

    List<VideoCategory> findByActiveTrueOrderByNameAsc();

    Optional<VideoCategory> findByIdAndActiveTrue(Long id);

    boolean existsByNameIgnoreCase(String name);
}
