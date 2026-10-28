package vn.edu.hcmute.fakebook.dto;

public record DirectMessageView(
    Long id,
    Long senderId,
    String senderUsername,
    String senderFullName,
    Long recipientId,
    String content,
    String createdAt,
    boolean read
) {
}