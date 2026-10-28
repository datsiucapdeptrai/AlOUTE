package vn.edu.hcmute.fakebook.controller;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import lombok.RequiredArgsConstructor;

import vn.edu.hcmute.fakebook.service.NotificationService;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAdvice {

    private final NotificationService notificationService;

    @ModelAttribute
    public void addCommonAttributes(
            Authentication authentication,
            Model model) {

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication
                    instanceof AnonymousAuthenticationToken) {

            return;
        }

        long unreadCount =
            notificationService.getUnreadCount(
                authentication.getName()
            );

        model.addAttribute(
            "unreadNotificationCount",
            unreadCount
        );
    }
}