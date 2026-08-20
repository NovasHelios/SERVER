package com.heilous.chat.dto;
import com.heilous.chat.entity.ChatRoom;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;
@Getter @Builder
public class ChatRoomResponse {
    private Long roomId; private Long landId; private String landAddress; private String counterpartEmail; private String counterpartName; private ChatRoom.Status status; private long unreadCount; private String lastMessage; private LocalDateTime lastMessageAt; private LocalDateTime createdAt;
    public static ChatRoomResponse from(ChatRoom room, String currentEmail, long unreadCount, String lastMessage, LocalDateTime lastMessageAt) {
        boolean company = room.getCompany().getEmail().equals(currentEmail);
        return ChatRoomResponse.builder().roomId(room.getId()).landId(room.getLand().getId()).landAddress(room.getLand().getAddress()).counterpartEmail(company ? room.getOwner().getEmail() : room.getCompany().getEmail()).counterpartName(company ? room.getOwner().getName() : room.getCompany().getName()).status(room.getStatus()).unreadCount(unreadCount).lastMessage(lastMessage).lastMessageAt(lastMessageAt).createdAt(room.getCreatedAt()).build();
    }
}
