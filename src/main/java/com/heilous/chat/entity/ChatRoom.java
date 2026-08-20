package com.heilous.chat.entity;

import com.heilous.common.entity.BaseEntity;
import com.heilous.land.entity.Land;
import com.heilous.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "chat_rooms", uniqueConstraints = @UniqueConstraint(name = "uk_chat_room_company_land", columnNames = {"company_id", "land_id"}), indexes = {
        @Index(name = "idx_chat_room_company_id", columnList = "company_id"),
        @Index(name = "idx_chat_room_owner_id", columnList = "owner_id")
})
@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor @Builder
public class ChatRoom extends BaseEntity {
    public enum Status { PENDING, ACCEPTED, REJECTED, CLOSED }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "land_id", nullable = false)
    private Land land;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "company_id", nullable = false)
    private User company;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;

    public void accept() { this.status = Status.ACCEPTED; }
    public void reject() { this.status = Status.REJECTED; }
    public void close() { this.status = Status.CLOSED; }
}
