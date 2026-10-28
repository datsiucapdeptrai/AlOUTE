package vn.edu.hcmute.fakebook.entity;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.hcmute.fakebook.enums.NotificationType;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Người gửi thông báo (người like, comment...)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private UserAccount sender;

    // Người nhận thông báo (chủ bài viết)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id")
    private UserAccount recipient;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private NotificationType type;

    @Column(columnDefinition = "NVARCHAR(500)")
    private String message;

    // ID của bài viết hoặc bình luận liên quan (để click vào thì nhảy đúng chỗ)
    private Long referenceId;

    // Đường link dẫn đến bài viết (VD: /feed#post-10)
    @Column(columnDefinition = "NVARCHAR(500)")
    private String targetUrl;

    // QUAN TRỌNG: Tên biến phải là 'read' để JPA hiểu hàm countByRecipientAndReadFalse
    @Column(name = "is_read")
    private boolean read = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // BỔ SUNG: Cột updated_at để tránh lỗi NULL từ SQL Server
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now(); // Gán thời gian tạo và cập nhật lần đầu
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now(); // Tự động cập nhật thời gian mỗi khi có sửa đổi (ví dụ: đánh dấu đã đọc)
    }
}