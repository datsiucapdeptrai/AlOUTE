package vn.edu.hcmute.fakebook.service;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import vn.edu.hcmute.fakebook.dto.FriendshipView;
import vn.edu.hcmute.fakebook.entity.Friendship;
import vn.edu.hcmute.fakebook.entity.UserAccount;
import vn.edu.hcmute.fakebook.enums.AccountStatus;
import vn.edu.hcmute.fakebook.enums.FriendshipStatus;
import vn.edu.hcmute.fakebook.enums.NotificationType;
import vn.edu.hcmute.fakebook.enums.RoleName; // THÊM IMPORT NÀY
import vn.edu.hcmute.fakebook.repository.FriendshipRepository;
import vn.edu.hcmute.fakebook.repository.UserAccountRepository;

@Service
@RequiredArgsConstructor
public class FriendshipService {

    private final FriendshipRepository friendshipRepository;
    private final UserAccountRepository userAccountRepository;
    private final NotificationService notificationService;

    // =====================================================
    // TÌM KIẾM NGƯỜI DÙNG
    // =====================================================
    @Transactional(readOnly = true)
    public List<FriendshipView> searchUsers(
            String currentUsername,
            String keyword) {

        UserAccount currentUser = findUser(currentUsername);
        String cleanKeyword = keyword == null ? "" : keyword.trim();

        return userAccountRepository.searchActiveUsers(
                currentUser.getId(),
                AccountStatus.ACTIVE,
                cleanKeyword,
                PageRequest.of(0, 20)
            )
            .stream()
            .map(user -> createSearchView(currentUser, user))
            .toList();
    }

    // =====================================================
    // LỜI MỜI KẾT BẠN ĐÃ NHẬN
    // =====================================================
    @Transactional(readOnly = true)
    public List<FriendshipView> getReceivedRequests(String username) {

        UserAccount currentUser = findUser(username);

        return friendshipRepository.findReceivedRequests(
                currentUser.getId(),
                FriendshipStatus.PENDING
            )
            .stream()
            .map(friendship -> createFriendshipView(currentUser, friendship))
            .toList();
    }

    // =====================================================
    // DANH SÁCH BẠN BÈ (trả về DTO)
    // =====================================================
    @Transactional(readOnly = true)
    public List<FriendshipView> getFriends(String username) {

        UserAccount currentUser = findUser(username);

        return friendshipRepository.findByUserAndStatus(
                currentUser.getId(),
                FriendshipStatus.ACCEPTED
            )
            .stream()
            .map(friendship -> createFriendshipView(currentUser, friendship))
            .toList();
    }

    // =====================================================
    // GỬI LỜI MỜI KẾT BẠN
    // =====================================================
    @Transactional
    public void sendRequest(
            String currentUsername,
            Long targetUserId) {

        UserAccount currentUser = findUser(currentUsername);

        UserAccount targetUser = userAccountRepository
            .findById(targetUserId)
            .orElseThrow(() ->
                new IllegalArgumentException("Không tìm thấy người dùng")
            );

        if (currentUser.getId().equals(targetUser.getId())) {
            throw new IllegalArgumentException(
                "Bạn không thể tự kết bạn với chính mình"
            );
        }

        UserAccount userOne =
            currentUser.getId() < targetUser.getId()
                ? currentUser
                : targetUser;

        UserAccount userTwo =
            currentUser.getId() < targetUser.getId()
                ? targetUser
                : currentUser;

        Friendship friendship = friendshipRepository
            .findByUserOneIdAndUserTwoId(userOne.getId(), userTwo.getId())
            .orElse(null);

        if (friendship == null) {
            friendship = new Friendship();
            friendship.setUserOne(userOne);
            friendship.setUserTwo(userTwo);

        } else if (friendship.getStatus() == FriendshipStatus.ACCEPTED) {
            throw new IllegalArgumentException("Hai người đã là bạn bè");

        } else if (friendship.getStatus() == FriendshipStatus.PENDING) {
            throw new IllegalArgumentException("Lời mời kết bạn đã tồn tại");

        } else if (friendship.getStatus() == FriendshipStatus.BLOCKED) {
            throw new IllegalArgumentException("Không thể gửi lời mời kết bạn");
        }

        friendship.setRequestedBy(currentUser);
        friendship.setStatus(FriendshipStatus.PENDING);

        Friendship saved = friendshipRepository.save(friendship);

        notificationService.sendNotification(
            currentUser,
            targetUser,
            NotificationType.FRIEND_REQUEST,
            "đã gửi cho bạn lời mời kết bạn.",
            saved.getId(),
            "/friends"
        );
    }

    // =====================================================
    // CHẤP NHẬN LỜI MỜI
    // =====================================================
    @Transactional
    public void acceptRequest(String username, Long friendshipId) {

        UserAccount currentUser = findUser(username);
        Friendship friendship = findFriendship(friendshipId);

        checkParticipant(currentUser, friendship);

        if (friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new IllegalArgumentException("Lời mời không còn hiệu lực");
        }

        if (friendship.getRequestedBy().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException(
                "Bạn không thể chấp nhận lời mời của chính mình"
            );
        }

        friendship.setStatus(FriendshipStatus.ACCEPTED);
        friendshipRepository.save(friendship);

        notificationService.sendNotification(
            currentUser,
            friendship.getRequestedBy(),
            NotificationType.FRIEND_ACCEPTED,
            "đã chấp nhận lời mời kết bạn của bạn.",
            friendship.getId(),
            "/friends"
        );
    }

    // =====================================================
    // TỪ CHỐI LỜI MỜI
    // =====================================================
    @Transactional
    public void rejectRequest(String username, Long friendshipId) {

        UserAccount currentUser = findUser(username);
        Friendship friendship = findFriendship(friendshipId);

        checkParticipant(currentUser, friendship);

        if (friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new IllegalArgumentException("Lời mời không còn hiệu lực");
        }

        if (friendship.getRequestedBy().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException(
                "Bạn không thể từ chối lời mời của chính mình"
            );
        }

        friendship.setStatus(FriendshipStatus.REJECTED);
        friendshipRepository.save(friendship);
    }

    // =====================================================
    // HỦY LỜI MỜI
    // =====================================================
    @Transactional
    public void cancelRequest(String username, Long friendshipId) {

        UserAccount currentUser = findUser(username);
        Friendship friendship = findFriendship(friendshipId);

        if (friendship.getStatus() != FriendshipStatus.PENDING
            || !friendship.getRequestedBy().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException(
                "Bạn không có quyền hủy lời mời này"
            );
        }

        friendship.setStatus(FriendshipStatus.REJECTED);
        friendshipRepository.save(friendship);
    }

    // =====================================================
    // HỦY KẾT BẠN
    // =====================================================
    @Transactional
    public void removeFriend(String username, Long friendshipId) {

        UserAccount currentUser = findUser(username);
        Friendship friendship = findFriendship(friendshipId);

        checkParticipant(currentUser, friendship);

        if (friendship.getStatus() != FriendshipStatus.ACCEPTED) {
            throw new IllegalArgumentException("Quan hệ bạn bè không tồn tại");
        }

        friendship.setStatus(FriendshipStatus.REJECTED);
        friendshipRepository.save(friendship);
    }

    // =====================================================
    // GỢI Ý KẾT BẠN - LẤY USER (ĐÃ CẬP NHẬT BỘ LỌC)
    // =====================================================
    @Transactional(readOnly = true)
    public List<UserAccount> getFriendSuggestionUsers(
            String currentUsername,
            int limit) {

        UserAccount currentUser = findUser(currentUsername);

        List<Long> friendIds = friendshipRepository
            .findAcceptedFriendIds(currentUser.getId());

        if (friendIds.isEmpty()) {
            return List.of();
        }

        // Lấy số lượng gợi ý nhiều hơn một chút (limit * 3) để sau khi lọc Admin/Manager vẫn đủ số lượng
        List<UserAccount> suggestions = friendshipRepository.findFriendSuggestions(
            currentUser.getId(),
            friendIds,
            PageRequest.of(0, limit * 3) 
        );

        // BỘ LỌC: Loại bỏ hoàn toàn Admin và Manager
        return suggestions.stream()
            .filter(user -> user.getRoles().stream().noneMatch(role -> 
                role.getName() == RoleName.ROLE_ADMIN || role.getName() == RoleName.ROLE_MANAGER
            ))
            .limit(limit)
            .toList();
    }

    // =====================================================
    // GỢI Ý KẾT BẠN - MAP userId -> số bạn chung (ĐÃ CẬP NHẬT BỘ LỌC)
    // =====================================================
    @Transactional(readOnly = true)
    public Map<Long, Long> getFriendSuggestions(
            String currentUsername,
            int limit) {

        UserAccount currentUser = findUser(currentUsername);

        List<Long> friendIds = friendshipRepository
            .findAcceptedFriendIds(currentUser.getId());

        if (friendIds.isEmpty()) {
            return Map.of();
        }

        // Áp dụng bộ lọc tương tự
        List<UserAccount> suggestions = friendshipRepository
            .findFriendSuggestions(
                currentUser.getId(),
                friendIds,
                PageRequest.of(0, limit * 3)
            )
            .stream()
            .filter(user -> user.getRoles().stream().noneMatch(role -> 
                role.getName() == RoleName.ROLE_ADMIN || role.getName() == RoleName.ROLE_MANAGER
            ))
            .limit(limit)
            .toList();

        Map<Long, Long> result = new LinkedHashMap<>();
        for (UserAccount suggestion : suggestions) {
            long mutualCount = countMutualFriends(friendIds, suggestion.getId());
            result.put(suggestion.getId(), mutualCount);
        }
        return result;
    }

    // =====================================================
    // ĐẾM SỐ BẠN CHUNG
    // =====================================================
    private long countMutualFriends(List<Long> friendIds, Long suggestionId) {
        if (friendIds.isEmpty()) return 0;

        long count = 0;
        for (Long friendId : friendIds) {
            Long one = Math.min(friendId, suggestionId);
            Long two = Math.max(friendId, suggestionId);
            if (friendshipRepository.findByUserOneIdAndUserTwoId(one, two)
                    .filter(f -> f.getStatus() == FriendshipStatus.ACCEPTED)
                    .isPresent()) {
                count++;
            }
        }
        return count;
    }

    // =====================================================
    // HELPERS
    // =====================================================
    private FriendshipView createSearchView(
            UserAccount currentUser,
            UserAccount displayedUser) {

        Long userOneId = Math.min(currentUser.getId(), displayedUser.getId());
        Long userTwoId = Math.max(currentUser.getId(), displayedUser.getId());

        Friendship friendship = friendshipRepository
            .findByUserOneIdAndUserTwoId(userOneId, userTwoId)
            .orElse(null);

        return FriendshipView.builder()
            .friendshipId(friendship == null ? null : friendship.getId())
            .userId(displayedUser.getId())
            .username(displayedUser.getUsername())
            .fullName(displayedUser.getFullName())
            .status(friendship == null ? null : friendship.getStatus())
            .requestedByCurrentUser(
                friendship != null
                && friendship.getRequestedBy().getId().equals(currentUser.getId())
            )
            .build();
    }

    private FriendshipView createFriendshipView(
            UserAccount currentUser,
            Friendship friendship) {

        UserAccount otherUser =
            friendship.getUserOne().getId().equals(currentUser.getId())
                ? friendship.getUserTwo()
                : friendship.getUserOne();

        return FriendshipView.builder()
            .friendshipId(friendship.getId())
            .userId(otherUser.getId())
            .username(otherUser.getUsername())
            .fullName(otherUser.getFullName())
            .status(friendship.getStatus())
            .requestedByCurrentUser(
                friendship.getRequestedBy().getId().equals(currentUser.getId())
            )
            .build();
    }

    private void checkParticipant(UserAccount user, Friendship friendship) {

        boolean participant =
            friendship.getUserOne().getId().equals(user.getId())
            || friendship.getUserTwo().getId().equals(user.getId());

        if (!participant) {
            throw new AccessDeniedException("Bạn không có quyền với quan hệ này");
        }
    }

    private UserAccount findUser(String username) {
        return userAccountRepository
            .findByUsernameIgnoreCase(username)
            .orElseThrow(() ->
                new IllegalStateException("Không tìm thấy người dùng")
            );
    }

    private Friendship findFriendship(Long id) {
        return friendshipRepository
            .findById(id)
            .orElseThrow(() ->
                new IllegalArgumentException("Không tìm thấy quan hệ bạn bè")
            );
    }
}