package vn.edu.hcmute.fakebook.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NotificationMessage {

    private Long id;

    private String type;

    private String message;

    private String targetUrl;

    private long unreadCount;
}