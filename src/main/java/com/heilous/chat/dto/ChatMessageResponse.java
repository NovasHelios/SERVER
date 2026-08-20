package com.heilous.chat.dto;
import com.heilous.chat.entity.ChatMessage;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;
@Getter @Builder
public class ChatMessageResponse {
    private Long messageId; private Long roomId; private String senderEmail; private String senderName; private String content; private String attachmentUrl; private String attachmentOriginalName; private LocalDateTime sentAt; private LocalDateTime readAt;
    public static ChatMessageResponse from(ChatMessage message) {
        String attachmentUrl = message.getAttachmentPath() == null ? null : "/uploads/chat/" + message.getAttachmentPath();
        return ChatMessageResponse.builder().messageId(message.getId()).roomId(message.getRoom().getId()).senderEmail(message.getSender().getEmail()).senderName(message.getSender().getName()).content(message.getContent()).attachmentUrl(attachmentUrl).attachmentOriginalName(message.getAttachmentOriginalName()).sentAt(message.getCreatedAt()).readAt(message.getReadAt()).build();
    }
}
