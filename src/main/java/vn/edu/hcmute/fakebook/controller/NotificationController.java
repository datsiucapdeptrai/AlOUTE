package vn.edu.hcmute.fakebook.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.RequiredArgsConstructor;

import vn.edu.hcmute.fakebook.service.NotificationService;

@Controller
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public String showNotifications(
            @RequestParam(
                name = "page",
                defaultValue = "0"
            )
            int page,
            Authentication authentication,
            Model model) {

        String username = authentication.getName();

        model.addAttribute(
            "notifications",
            notificationService.getNotifications(
                username,
                page
            )
        );

        model.addAttribute(
            "unreadNotificationCount",
            notificationService.getUnreadCount(username)
        );

        return "notifications/index";
    }

    @PostMapping("/{id}/read")
    public String markAsRead(
            @PathVariable("id") Long id,
            Authentication authentication) {

        String targetUrl =
            notificationService.markAsRead(
                id,
                authentication.getName()
            );

        return "redirect:" + targetUrl;
    }

    @PostMapping("/read-all")
    public String markAllAsRead(
            Authentication authentication) {

        notificationService.markAllAsRead(
            authentication.getName()
        );

        return "redirect:/notifications";
    }
}