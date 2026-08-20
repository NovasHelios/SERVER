package com.heilous.chat.service;

import com.heilous.chat.dto.ChatMessageResponse;
import com.heilous.chat.dto.ChatRoomResponse;
import com.heilous.chat.entity.ChatMessage;
import com.heilous.chat.entity.ChatRoom;
import com.heilous.chat.repository.ChatMessageRepository;
import com.heilous.chat.repository.ChatRoomRepository;
import com.heilous.common.exception.CustomException;
import com.heilous.common.exception.GlobalErrorCode;
import com.heilous.common.service.ImageStorageService;
import com.heilous.land.entity.Land;
import com.heilous.land.repository.LandRepository;
import com.heilous.user.entity.User;
import com.heilous.user.enums.UserRole;
import com.heilous.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final LandRepository landRepository;
    private final UserRepository userRepository;
    private final ImageStorageService imageStorageService;

    @Transactional
    public ChatRoomResponse createRoom(Long landId, String initialMessage, String email) {
        User company = getUser(email);
        if (company.getRole() != UserRole.COMPANY) throw new CustomException(GlobalErrorCode.ACCESS_DENIED);
        Land land = landRepository.findById(landId).orElseThrow(() -> new CustomException(GlobalErrorCode.LAND_NOT_FOUND));
        if (chatRoomRepository.existsByCompanyIdAndLandId(company.getId(), landId)) throw new CustomException(GlobalErrorCode.CHAT_ROOM_ALREADY_EXISTS);
        ChatRoom room = chatRoomRepository.save(ChatRoom.builder().land(land).company(company).owner(land.getOwner()).build());
        ChatMessage message = null;
        if (initialMessage != null && !initialMessage.isBlank()) {
            message = chatMessageRepository.save(ChatMessage.builder().room(room).sender(company).content(initialMessage.trim()).build());
        }
        return toRoomResponse(room, email, message);
    }

    @Transactional(readOnly = true)
    public List<ChatRoomResponse> getRooms(String email) {
        getUser(email);
        return chatRoomRepository.findAllByParticipantEmail(email).stream().map(room -> toRoomResponse(room, email, null)).toList();
    }

    @Transactional
    public List<ChatMessageResponse> getMessages(Long roomId, String email) {
        ChatRoom room = requireParticipant(roomId, email);
        markMessagesAsRead(room, email);
        return chatMessageRepository.findAllByRoomIdOrderByIdAsc(roomId).stream().map(ChatMessageResponse::from).toList();
    }

    @Transactional
    public ChatMessageResponse sendMessage(Long roomId, String email, String content) {
        ChatRoom room = requireParticipant(roomId, email);
        if (room.getStatus() != ChatRoom.Status.ACCEPTED) throw new CustomException(GlobalErrorCode.CHAT_ROOM_NOT_ACTIVE);
        User sender = getUser(email);
        ChatMessage message = chatMessageRepository.save(ChatMessage.builder().room(room).sender(sender).content(content.trim()).build());
        return ChatMessageResponse.from(message);
    }

    @Transactional
    public ChatMessageResponse sendAttachment(Long roomId, String email, MultipartFile file) {
        ChatRoom room = requireParticipant(roomId, email);
        if (room.getStatus() != ChatRoom.Status.ACCEPTED) throw new CustomException(GlobalErrorCode.CHAT_ROOM_NOT_ACTIVE);
        User sender = getUser(email);
        String attachmentPath = imageStorageService.storeDocument(file, "chat");
        String originalName = file.getOriginalFilename() == null ? attachmentPath : file.getOriginalFilename();
        ChatMessage message = chatMessageRepository.save(ChatMessage.builder()
                .room(room).sender(sender).content(originalName).attachmentPath(attachmentPath).attachmentOriginalName(originalName).build());
        return ChatMessageResponse.from(message);
    }

    @Transactional
    public ChatRoomResponse acceptRoom(Long roomId, String email) {
        ChatRoom room = requireParticipant(roomId, email);
        if (!room.getOwner().getEmail().equals(email)) throw new CustomException(GlobalErrorCode.CHAT_ROOM_ACCESS_DENIED);
        if (room.getStatus() != ChatRoom.Status.PENDING) throw new CustomException(GlobalErrorCode.CHAT_ROOM_NOT_ACTIVE);
        room.accept();
        return toRoomResponse(room, email, null);
    }

    @Transactional
    public ChatRoomResponse rejectRoom(Long roomId, String email) {
        ChatRoom room = requireParticipant(roomId, email);
        if (!room.getOwner().getEmail().equals(email)) throw new CustomException(GlobalErrorCode.CHAT_ROOM_ACCESS_DENIED);
        if (room.getStatus() != ChatRoom.Status.PENDING) throw new CustomException(GlobalErrorCode.CHAT_ROOM_NOT_ACTIVE);
        room.reject();
        return toRoomResponse(room, email, null);
    }

    @Transactional
    public void closeRoom(Long roomId, String email) {
        ChatRoom room = requireParticipant(roomId, email);
        if (room.getStatus() != ChatRoom.Status.ACCEPTED) throw new CustomException(GlobalErrorCode.CHAT_ROOM_NOT_ACTIVE);
        room.close();
    }

    private ChatRoom requireParticipant(Long roomId, String email) {
        ChatRoom room = chatRoomRepository.findDetailById(roomId).orElseThrow(() -> new CustomException(GlobalErrorCode.CHAT_ROOM_NOT_FOUND));
        if (!room.getCompany().getEmail().equals(email) && !room.getOwner().getEmail().equals(email)) throw new CustomException(GlobalErrorCode.CHAT_ROOM_ACCESS_DENIED);
        return room;
    }
    @Transactional(readOnly = true)
    public void validateParticipant(Long roomId, String email) { requireParticipant(roomId, email); }
    private void markMessagesAsRead(ChatRoom room, String email) {
        chatMessageRepository.findAllByRoomIdOrderByIdAsc(room.getId()).stream()
                .filter(message -> !message.getSender().getEmail().equals(email))
                .forEach(ChatMessage::markAsRead);
    }
    private ChatRoomResponse toRoomResponse(ChatRoom room, String email, ChatMessage latestMessage) {
        ChatMessage latest = latestMessage != null ? latestMessage : chatMessageRepository.findTopByRoomIdOrderByIdDesc(room.getId()).orElse(null);
        long unread = chatMessageRepository.countUnreadByRoomIdAndReceiverEmail(room.getId(), email);
        return ChatRoomResponse.from(room, email, unread, latest == null ? null : latest.getContent(), latest == null ? null : latest.getCreatedAt());
    }
    private User getUser(String email) { return userRepository.findByEmail(email).orElseThrow(() -> new CustomException(GlobalErrorCode.USER_NOT_FOUND)); }
}
