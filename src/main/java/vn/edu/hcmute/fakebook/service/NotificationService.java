package vn.edu.hcmute.fakebook.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import vn.edu.hcmute.fakebook.dto.NotificationMessage;
import vn.edu.hcmute.fakebook.entity.Notification;
import vn.edu.hcmute.fakebook.entity.UserAccount;
import vn.edu.hcmute.fakebook.enums.NotificationType;
import vn.edu.hcmute.fakebook.repository.NotificationRepository;
import vn.edu.hcmute.fakebook.repository.UserAccountRepository;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final int PAGE_SIZE = 15;

    private final NotificationRepository notificationRepository;

    private final UserAccountRepository userAccountRepository;

    private final SimpMessagingTemplate messagingTemplate;
   

    @Transactional
    public void sendNotification(
            UserAccount actor,
            UserAccount recipient,
            NotificationType type,
            String message,
            Long referenceId,
            String targetUrl) {

        if (actor.getId().equals(recipient.getId())) {
            return;
        }

        Notification notification = new Notification();

        notification.setActor(actor);
        notification.setRecipient(recipient);
        notification.setType(type);
        notification.setMessage(message);
        notification.setReferenceId(referenceId);
        notification.setTargetUrl(targetUrl);
        notification.setRead(false);

        Notification saved =
            notificationRepository.save(notification);

        long unreadCount =
            notificationRepository
                .countByRecipientAndReadFalse(recipient);

        NotificationMessage socketMessage =
            new NotificationMessage(
                saved.getId(),
                type.name(),
                message,
                targetUrl,
                unreadCount
            );

        messagingTemplate.convertAndSendToUser(
            recipient.getUsername(),
            "/queue/notifications",
            socketMessage
        );
    }

    @Transactional(readOnly = true)
    public Page<Notification> getNotifications(
            String username,
            int page) {

        UserAccount user = findUser(username);

        return notificationRepository
            .findByRecipientOrderByCreatedAtDesc(
                user,
                PageRequest.of(Math.max(page, 0), PAGE_SIZE)
            );
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(String username) {

        UserAccount user = findUser(username);

        return notificationRepository
            .countByRecipientAndReadFalse(user);
    }

    @Transactional
    public String markAsRead(
            Long notificationId,
            String username) {

        Notification notification = notificationRepository
            .findById(notificationId)
            .orElseThrow(() ->
                new IllegalArgumentException(
                    "Không tìm thấy thông báo"
                )
            );

        if (!notification.getRecipient()
                .getUsername()
                .equalsIgnoreCase(username)) {

            throw new AccessDeniedException(
                "Bạn không có quyền xem thông báo này"
            );
        }

        notification.setRead(true);

        String targetUrl = notification.getTargetUrl();

        if (targetUrl == null
                || !targetUrl.startsWith("/")
                || targetUrl.startsWith("//")) {

            return "/notifications";
        }

        return targetUrl;
    }

    @Transactional
    public void markAllAsRead(String username) {

        UserAccount user = findUser(username);

        List<Notification> notifications =
            notificationRepository
                .findByRecipientAndReadFalse(user);

        notifications.forEach(
            notification -> notification.setRead(true)
        );
    }

    private UserAccount findUser(String username) {

        return userAccountRepository
            .findByUsernameIgnoreCase(username)
            .orElseThrow(() ->
                new IllegalStateException(
                    "Không tìm thấy người dùng"
                )
            );
    }
}