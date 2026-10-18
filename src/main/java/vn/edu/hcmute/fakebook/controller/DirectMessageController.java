package vn.edu.hcmute.fakebook.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.RequiredArgsConstructor;
import vn.edu.hcmute.fakebook.dto.DirectMessageView;
import vn.edu.hcmute.fakebook.entity.UserAccount;
import vn.edu.hcmute.fakebook.repository.UserAccountRepository;
import vn.edu.hcmute.fakebook.service.DirectMessageService;
import vn.edu.hcmute.fakebook.service.FriendshipService;

@Controller
@RequestMapping("/messages")
@RequiredArgsConstructor
public class DirectMessageController {

    private static final Logger log = LoggerFactory.getLogger(DirectMessageController.class);

    private final DirectMessageService directMessageService;
    private final FriendshipService friendshipService;
    private final UserAccountRepository userAccountRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @GetMapping
    public String index(Authentication authentication, Model model) {
        model.addAttribute(
            "friends",
            friendshipService.getFriends(authentication.getName())
        );
        model.addAttribute(
            "unreadCounts",
            directMessageService.getUnreadCounts(authentication.getName())
        );
        return "messages/index";
    }

    @GetMapping("/unread/count")
    public ResponseEntity<Map<String, Long>> unreadCount(
            Authentication authentication) {
        long total = directMessageService.getUnreadCounts(
                authentication.getName()
            )
            .values()
            .stream()
            .mapToLong(Long::longValue)
            .sum();

        return ResponseEntity.ok(Map.of("unreadCount", total));
    }

    @GetMapping("/api/contacts")
    public ResponseEntity<Map<String, Object>> contacts(
            Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(Map.of(
            "friends", friendshipService.getFriends(username),
            "unreadCounts", directMessageService.getUnreadCounts(username)
        ));
    }

    @GetMapping("/api/conversation/{otherUserId}")
    public ResponseEntity<?> conversationJson(
            @PathVariable("otherUserId") Long otherUserId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            Authentication authentication) {
        try {
            Page<DirectMessageView> conversation = directMessageService
                .getConversation(authentication.getName(), otherUserId, page);
            return ResponseEntity.ok(Map.of(
                "messages", conversation.getContent(),
                "page", conversation.getNumber(),
                "hasOlder", conversation.hasNext()
            ));
        } catch (AccessDeniedException exception) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(errorResponse(exception.getMessage()));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest()
                .body(errorResponse(exception.getMessage()));
        }
    }

    @GetMapping("/{otherUserId}")
    public String conversation(
            @PathVariable("otherUserId") Long otherUserId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            Authentication authentication,
            Model model) {

        String username = authentication.getName();
        // getConversation() kiểm tra người xem có quyền đọc cuộc trò chuyện.
        model.addAttribute(
            "conversation",
            directMessageService.getConversation(username, otherUserId, page)
        );
        model.addAttribute("friends", friendshipService.getFriends(username));
        model.addAttribute(
            "unreadCounts",
            directMessageService.getUnreadCounts(username)
        );

        UserAccount currentUser = userAccountRepository
            .findByUsernameIgnoreCase(username)
            .orElseThrow(() ->
                new IllegalStateException("Không tìm thấy người dùng")
            );
        UserAccount otherUser = userAccountRepository
            .findById(otherUserId)
            .orElseThrow(() ->
                new IllegalArgumentException("Không tìm thấy người nhận")
            );
        model.addAttribute("currentUserId", currentUser.getId());
        model.addAttribute("otherUser", otherUser);
        return "messages/index";
    }

    @PostMapping(value = "/{otherUserId}",
                 headers = "X-Requested-With=XMLHttpRequest")
    public ResponseEntity<?> sendAjax(
            @PathVariable("otherUserId") Long otherUserId,
            @RequestParam("content") String content,
            Authentication authentication) {

        try {
            DirectMessageView sent = sendAndNotify(
                authentication.getName(), otherUserId, content
            );
            return ResponseEntity.ok(sent);
        } catch (AccessDeniedException exception) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(errorResponse(exception.getMessage()));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest()
                .body(errorResponse(exception.getMessage()));
        }
    }

    @PostMapping("/{otherUserId}")
    public String send(
            @PathVariable("otherUserId") Long otherUserId,
            @RequestParam("content") String content,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        try {
            sendAndNotify(
                authentication.getName(), otherUserId, content
            );
        } catch (IllegalArgumentException | AccessDeniedException exception) {
            redirectAttributes.addFlashAttribute(
                "messageError", exception.getMessage()
            );
        }
        return "redirect:/messages/" + otherUserId;
    }

    @PostMapping("/{otherUserId}/read")
    public ResponseEntity<?> markRead(
            @PathVariable("otherUserId") Long otherUserId,
            Authentication authentication) {
        try {
            markReadAndNotify(authentication.getName(), otherUserId);
            return ResponseEntity.noContent().build();
        } catch (AccessDeniedException exception) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(errorResponse(exception.getMessage()));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest()
                .body(errorResponse(exception.getMessage()));
        }
    }

    private void markReadAndNotify(String username, Long otherUserId) {
        Long lastReadId = directMessageService.markRead(username, otherUserId);
        if (lastReadId == null) return;

        UserAccount reader = userAccountRepository.findByUsernameIgnoreCase(username)
            .orElseThrow(() -> new IllegalStateException("Không tìm thấy người dùng"));
        String senderUsername = userAccountRepository.findById(otherUserId)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người gửi"))
            .getUsername();

        try {
            messagingTemplate.convertAndSendToUser(
                senderUsername,
                "/queue/messages-read",
                Map.of("readerId", reader.getId(), "readUpToId", lastReadId)
            );
        } catch (MessagingException exception) {
            log.warn("Không thể phát trạng thái đã xem cho người dùng id={}",
                otherUserId, exception);
        }
    }

    private DirectMessageView sendAndNotify(
            String username,
            Long otherUserId,
            String content) {

        String recipientUsername = userAccountRepository.findById(otherUserId)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người nhận"))
            .getUsername();

        // send() đã hoàn tất giao dịch lưu DB trước khi phát WebSocket.
        DirectMessageView sent = directMessageService.send(
            username, otherUserId, content
        );
        try {
            messagingTemplate.convertAndSendToUser(
                recipientUsername, "/queue/messages", sent
            );
        } catch (MessagingException exception) {
            // Tin đã lưu; lỗi phát WebSocket không được làm người gửi gửi lại tin trùng.
            log.warn("Không thể phát trực tiếp tin nhắn id={}", sent.id(), exception);
        }
        return sent;
    }

    private Map<String, String> errorResponse(String message) {
        return Map.of(
            "message",
            message == null ? "Không thể gửi tin nhắn." : message
        );
    }
}
