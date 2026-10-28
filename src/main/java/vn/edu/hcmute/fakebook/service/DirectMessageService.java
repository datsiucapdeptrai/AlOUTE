package vn.edu.hcmute.fakebook.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import vn.edu.hcmute.fakebook.dto.DirectMessageView;
import vn.edu.hcmute.fakebook.entity.DirectMessage;
import vn.edu.hcmute.fakebook.entity.Friendship;
import vn.edu.hcmute.fakebook.entity.UserAccount;
import vn.edu.hcmute.fakebook.enums.AccountStatus;
import vn.edu.hcmute.fakebook.enums.FriendshipStatus;
import vn.edu.hcmute.fakebook.repository.DirectMessageRepository;
import vn.edu.hcmute.fakebook.repository.FriendshipRepository;
import vn.edu.hcmute.fakebook.repository.UserAccountRepository;

@Service
@RequiredArgsConstructor
public class DirectMessageService {

    private static final int PAGE_SIZE = 20;
    private static final int MAX_CONTENT_LENGTH = 2000;

    private final DirectMessageRepository directMessageRepository;
    private final UserAccountRepository userAccountRepository;
    private final FriendshipRepository friendshipRepository;

    @Transactional
    public DirectMessageView send(
            String currentUsername,
            Long recipientId,
            String content) {

        UserAccount sender = findCurrentUser(currentUsername);
        UserAccount recipient = findUserById(recipientId);

        requireDifferentUsers(sender, recipient);
        requireActiveRecipient(recipient);
        requireFriendship(sender, recipient);

        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Nội dung tin nhắn không được để trống"
            );
        }

        String cleanContent = content.trim();
        if (cleanContent.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException(
                "Tin nhắn không được vượt quá 2000 ký tự"
            );
        }

        DirectMessage message = new DirectMessage();
        message.setSender(sender);
        message.setRecipient(recipient);
        message.setContent(cleanContent);
        message.setRead(false);

        DirectMessage saved = directMessageRepository.saveAndFlush(message);
        return toView(saved);
    }

    @Transactional(readOnly = true)
    public Page<DirectMessageView> getConversation(
            String currentUsername,
            Long otherUserId,
            int page) {

        UserAccount currentUser = findCurrentUser(currentUsername);
        UserAccount otherUser = findUserById(otherUserId);

        requireDifferentUsers(currentUser, otherUser);
        requireFriendship(currentUser, otherUser);

        return directMessageRepository.findConversation(
                currentUser.getId(),
                otherUser.getId(),
                PageRequest.of(Math.max(page, 0), PAGE_SIZE)
            )
            .map(this::toView);
    }

    @Transactional
    public Long markRead(String currentUsername, Long otherUserId) {
        UserAccount currentUser = findCurrentUser(currentUsername);
        UserAccount otherUser = findUserById(otherUserId);

        requireDifferentUsers(currentUser, otherUser);
        requireFriendship(currentUser, otherUser);

        int updated = directMessageRepository.markReadFromSender(
            otherUser.getId(),
            currentUser.getId()
        );

        if (updated == 0) {
            return null;
        }

        return directMessageRepository.findLastReadId(
            otherUser.getId(),
            currentUser.getId()
        );
    }

    @Transactional(readOnly = true)
    public Map<Long, Long> getUnreadCounts(String currentUsername) {
        UserAccount currentUser = findCurrentUser(currentUsername);
        Map<Long, Long> counts = new HashMap<>();

        for (Object[] row : directMessageRepository.countUnreadBySender(
                currentUser.getId())) {
            Long senderId = ((Number) row[0]).longValue();
            Long unreadCount = ((Number) row[1]).longValue();
            counts.put(senderId, unreadCount);
        }

        return counts;
    }

    private void requireDifferentUsers(
            UserAccount first,
            UserAccount second) {

        if (first.getId().equals(second.getId())) {
            throw new IllegalArgumentException(
                "Bạn không thể nhắn tin cho chính mình"
            );
        }
    }

    private void requireActiveRecipient(UserAccount recipient) {
        if (recipient.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalArgumentException(
                "Tài khoản nhận tin hiện không hoạt động"
            );
        }
    }

    private void requireFriendship(
            UserAccount first,
            UserAccount second) {

        Long userOneId = Math.min(first.getId(), second.getId());
        Long userTwoId = Math.max(first.getId(), second.getId());

        Friendship friendship = friendshipRepository
            .findByUserOneIdAndUserTwoId(userOneId, userTwoId)
            .orElseThrow(() ->
                new AccessDeniedException(
                    "Chỉ bạn bè mới có thể nhắn tin với nhau"
                )
            );

        if (friendship.getStatus() != FriendshipStatus.ACCEPTED) {
            throw new AccessDeniedException(
                "Chỉ bạn bè mới có thể nhắn tin với nhau"
            );
        }
    }

    private UserAccount findCurrentUser(String username) {
        return userAccountRepository.findByUsernameIgnoreCase(username)
            .orElseThrow(() ->
                new IllegalStateException("Không tìm thấy người dùng")
            );
    }

    private UserAccount findUserById(Long id) {
        return userAccountRepository.findById(id)
            .orElseThrow(() ->
                new IllegalArgumentException(
                    "Không tìm thấy người nhận"
                )
            );
    }

    private DirectMessageView toView(DirectMessage message) {
        UserAccount sender = message.getSender();

        return new DirectMessageView(
            message.getId(),
            sender.getId(),
            sender.getUsername(),
            sender.getFullName(),
            message.getRecipient().getId(),
            message.getContent(),
            message.getCreatedAt() == null
                ? "" : message.getCreatedAt().toString(),
            message.isRead()
        );
    }
}