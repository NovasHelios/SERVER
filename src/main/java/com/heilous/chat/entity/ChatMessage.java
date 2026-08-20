package com.heilous.chat.entity;

import com.heilous.common.entity.BaseEntity;
import com.heilous.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages", indexes = @Index(name = "idx_chat_message_room_id_id", columnList = "room_id, id"))
@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor @Builder
public class ChatMessage extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "room_id", nullable = false)
    private ChatRoom room;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "sender_id", nullable = false)
    private User sender;
    @Column(nullable = false, length = 2000)
    private String content;

    private String attachmentPath;

    private String attachmentOriginalName;

    private LocalDateTime readAt;

    public void markAsRead() { if (readAt == null) this.readAt = LocalDateTime.now(); }
}
