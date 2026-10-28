package vn.edu.hcmute.fakebook.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import lombok.RequiredArgsConstructor;
import vn.edu.hcmute.fakebook.service.FriendshipService;

@Controller
@RequiredArgsConstructor
public class FriendshipController {

    private final FriendshipService friendshipService;

    @GetMapping("/friends")
    public String showFriends(
            @RequestParam(name = "q", defaultValue = "") String keyword,
            Authentication authentication,
            Model model) {

        String username = authentication.getName();

        model.addAttribute(
            "searchResults",
            friendshipService.searchUsers(username, keyword)
        );

        model.addAttribute(
            "receivedRequests",
            friendshipService.getReceivedRequests(username)
        );

        model.addAttribute(
            "friends",
            friendshipService.getFriends(username)
        );

        model.addAttribute("keyword", keyword);

        return "friends/index";
    }

    // =====================================================
    // GỬI LỜI MỜI KẾT BẠN (form POST truyền thống)
    // =====================================================
    @PostMapping("/friends/request/{userId}")
    public String sendRequest(
            @PathVariable Long userId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        return execute(
            () -> friendshipService.sendRequest(
                authentication.getName(),
                userId
            ),
            "Đã gửi lời mời kết bạn.",
            redirectAttributes
        );
    }

    // =====================================================
    // GỬI LỜI MỜI KẾT BẠN QUA AJAX (trả JSON)
    // Dùng cho nút "Kết bạn" trong sidebar gợi ý
    // =====================================================
    @PostMapping(value = "/friends/request/{userId}",
                 headers = "X-Requested-With=XMLHttpRequest")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> sendRequestAjax(
            @PathVariable Long userId,
            Authentication authentication) {

        try {
            friendshipService.sendRequest(
                authentication.getName(),
                userId
            );

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Đã gửi lời mời kết bạn."
            ));

        } catch (IllegalArgumentException | AccessDeniedException exception) {
            return ResponseEntity.badRequest()
                .body(Map.of(
                    "success", false,
                    "message", exception.getMessage()
                ));

        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "message", "Không thể gửi lời mời kết bạn."
                ));
        }
    }

    @PostMapping("/friends/{id}/accept")
    public String acceptRequest(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        return execute(
            () -> friendshipService.acceptRequest(
                authentication.getName(),
                id
            ),
            "Đã chấp nhận lời mời kết bạn.",
            redirectAttributes
        );
    }

    @PostMapping("/friends/{id}/reject")
    public String rejectRequest(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        return execute(
            () -> friendshipService.rejectRequest(
                authentication.getName(),
                id
            ),
            "Đã từ chối lời mời kết bạn.",
            redirectAttributes
        );
    }

    @PostMapping("/friends/{id}/cancel")
    public String cancelRequest(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        return execute(
            () -> friendshipService.cancelRequest(
                authentication.getName(),
                id
            ),
            "Đã thu hồi lời mời kết bạn.",
            redirectAttributes
        );
    }

    @PostMapping("/friends/{id}/remove")
    public String removeFriend(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        return execute(
            () -> friendshipService.removeFriend(
                authentication.getName(),
                id
            ),
            "Đã hủy kết bạn.",
            redirectAttributes
        );
    }

    private String execute(
            Runnable action,
            String successMessage,
            RedirectAttributes redirectAttributes) {

        try {
            action.run();
            redirectAttributes.addFlashAttribute("message", successMessage);
        } catch (IllegalArgumentException | AccessDeniedException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }

        return "redirect:/friends";
    }
}