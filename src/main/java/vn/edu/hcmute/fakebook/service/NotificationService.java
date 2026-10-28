package vn.edu.hcmute.fakebook.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hcmute.fakebook.entity.Notification;
import vn.edu.hcmute.fakebook.entity.UserAccount;
import vn.edu.hcmute.fakebook.enums.NotificationType;
import vn.edu.hcmute.fakebook.repository.NotificationRepository;
import vn.edu.hcmute.fakebook.repository.UserAccountRepository;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserAccountRepository userAccountRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // 1. Hàm gửi thông báo (Realtime + Lưu DB)
    public void sendNotification(UserAccount sender, UserAccount receiver, NotificationType type, 
                                 String message, Long referenceId, String targetUrl) {
        
        if (sender.getId().equals(receiver.getId())) return;

        Notification notification = new Notification();
        notification.setSender(sender);
        notification.setRecipient(receiver);
        notification.setType(type);
        notification.setMessage(sender.getFullName() + " " + message);
        notification.setReferenceId(referenceId);
        notification.setTargetUrl(targetUrl);
        notification.setRead(false);
        notificationRepository.save(notification);

        Map<String, Object> wsPayload = new HashMap<>();
        wsPayload.put("senderName", sender.getFullName());
        wsPayload.put("message", message);
        wsPayload.put("targetUrl", targetUrl);
        
        messagingTemplate.convertAndSendToUser(receiver.getUsername(), "/queue/notifications", wsPayload);
    }

    // 2. Hàm lấy số lượng chưa đọc (Cho Badge đỏ)
    public long getUnreadCount(String username) {
        UserAccount user = userAccountRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));
        return notificationRepository.countByRecipientAndReadFalse(user);
    }

    // 3. Hàm lấy danh sách thông báo
    public Page<Notification> getNotifications(String username, int page) {
        UserAccount user = userAccountRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));
        return notificationRepository.findByRecipientOrderByCreatedAtDesc(user, PageRequest.of(Math.max(page, 0), 10));
    }

    // 4. Hàm đánh dấu 1 thông báo đã đọc (Trả về link để Controller redirect)
    @Transactional
    public String markAsRead(Long id, String username) {
        UserAccount user = userAccountRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));
        
        Notification notification = notificationRepository.findByIdAndRecipient(id, user)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông báo"));
        
        notification.setRead(true);
        notificationRepository.save(notification);
        return notification.getTargetUrl();
    }

    // 5. Hàm đánh dấu tất cả đã đọc
    @Transactional
    public void markAllAsRead(String username) {
        UserAccount user = userAccountRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));
        // Lấy tất cả thông báo chưa đọc và set thành đã đọc
        Page<Notification> page = notificationRepository.findByRecipientOrderByCreatedAtDesc(user, PageRequest.of(0, 1000));
        for (Notification n : page.getContent()) {
            if (!n.isRead()) {
                n.setRead(true);
                notificationRepository.save(n);
            }
        }
    }
}