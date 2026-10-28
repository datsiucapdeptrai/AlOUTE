package vn.edu.hcmute.fakebook.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.hcmute.fakebook.entity.Notification;
import vn.edu.hcmute.fakebook.entity.UserAccount;

import java.util.Optional; // Thêm import này

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    // Đếm số thông báo chưa đọc
    long countByRecipientAndReadFalse(UserAccount recipient);
    
    // Lấy danh sách thông báo
    @EntityGraph(attributePaths = {"sender", "sender.profile"})
    Page<Notification> findByRecipientOrderByCreatedAtDesc(UserAccount recipient, Pageable pageable);
    
    // THÊM DÒNG NÀY VÀO ĐÂY (Hàm này đang bị thiếu)
    Optional<Notification> findByIdAndRecipient(Long id, UserAccount recipient);
}