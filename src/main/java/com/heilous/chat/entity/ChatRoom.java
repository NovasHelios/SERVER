package com.heilous.chat.entity;

import com.heilous.common.entity.BaseEntity;
import com.heilous.land.entity.Land;
import com.heilous.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "chat_rooms", indexes = {
        @Index(name = "idx_chat_room_company_id", columnList = "company_id"),
        @Index(name = "idx_chat_room_owner_id", columnList = "owner_id")
})
@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor @Builder
public class ChatRoom extends BaseEntity {
    public enum Status { PENDING, ACCEPTED, REJECTED, CLOSED }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 토지 연결 채팅방 — 토지 없는 1:1 채팅 시 null */
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "land_id")
    private Land land;

    /** BUSINESS 플랜 사용자 (채팅 요청자) */
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "company_id", nullable = false)
    private User company;

    /** 상대방 (토지 소유자 또는 일반 유저) */
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.ACCEPTED;

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatMessage> messages = new ArrayList<>();

    public void accept() { this.status = Status.ACCEPTED; }
    public void reject() { this.status = Status.REJECTED; }
    public void close()  { this.status = Status.CLOSED; }
}
