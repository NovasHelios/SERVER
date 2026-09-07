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
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private static final int DEFAULT_PAGE_SIZE = 50;

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final LandRepository landRepository;
    private final UserRepository userRepository;
    private final ImageStorageService imageStorageService;

    // ─────────────────────────────────────────────
    // 채팅방 생성
    // ─────────────────────────────────────────────
    @Transactional
    public ChatRoomResponse createRoom(Long landId, String initialMessage, String email) {
        User company = getUser(email);
        if (company.getRole() != UserRole.COMPANY) {
            throw new CustomException(GlobalErrorCode.ACCESS_DENIED);
        }
        Land land = landRepository.findById(landId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.LAND_NOT_FOUND));
        if (chatRoomRepository.existsByCompanyIdAndLandId(company.getId(), landId)) {
            throw new CustomException(GlobalErrorCode.CHAT_ROOM_ALREADY_EXISTS);
        }

        ChatRoom room = chatRoomRepository.save(
                ChatRoom.builder().land(land).company(company).owner(land.getOwner()).build()
        );

        ChatMessage message = null;
        if (initialMessage != null && !initialMessage.isBlank()) {
            message = chatMessageRepository.save(
                    ChatMessage.builder()
                            .room(room).sender(company)
                            .content(initialMessage.trim())
                            .build()
            );
        }
        return toRoomResponse(room, email, message);
    }

    // ─────────────────────────────────────────────
    // 채팅방 목록 조회 (N+1 해결: 배치 쿼리 사용)
    // ─────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<ChatRoomResponse> getRooms(String email) {
        getUser(email);
        List<ChatRoom> rooms = chatRoomRepository.findAllByParticipantEmail(email);
        if (rooms.isEmpty()) return Collections.emptyList();

        List<Long> roomIds = rooms.stream().map(ChatRoom::getId).toList();

        // 최신 메시지 배치 조회 (방 수만큼 쿼리 X → 1번 쿼리)
        Map<Long, ChatMessage> latestMessageMap = chatMessageRepository
                .findLatestMessagesByRoomIds(roomIds)
                .stream()
                .collect(Collectors.toMap(m -> m.getRoom().getId(), m -> m));

        // 미읽음 수 배치 집계 (방 수만큼 쿼리 X → 1번 쿼리)
        Map<Long, Long> unreadCountMap = chatMessageRepository
                .countUnreadGroupByRoomIds(roomIds, email)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));

        return rooms.stream().map(room -> {
            ChatMessage latest = latestMessageMap.get(room.getId());
            long unread = unreadCountMap.getOrDefault(room.getId(), 0L);
            return ChatRoomResponse.from(
                    room, email, unread,
                    latest == null ? null : latest.getContent(),
                    latest == null ? null : latest.getCreatedAt()
            );
        }).toList();
    }

    // ─────────────────────────────────────────────
    // 메시지 조회 + 읽음 처리 (커서 기반 페이지네이션)
    // cursorId=0 이면 최신 DEFAULT_PAGE_SIZE 개를 반환
    // ─────────────────────────────────────────────
    @Transactional
    public List<ChatMessageResponse> getMessages(Long roomId, String email, Long cursorId, int size) {
        requireParticipant(roomId, email);

        // bulk 읽음 처리 (dirty checking 대신 UPDATE 단일 쿼리)
        chatMessageRepository.markAllAsRead(roomId, email, LocalDateTime.now());

        List<ChatMessage> messages;
        if (cursorId == null || cursorId == 0) {
            // 최초 조회: 최신 size개를 역순으로 가져와서 오름차순으로 뒤집기
            List<ChatMessage> latest = chatMessageRepository.findByRoomIdLatest(
                    roomId, PageRequest.of(0, size)
            );
            messages = latest.reversed();
        } else {
            messages = chatMessageRepository.findByRoomIdAfterCursor(
                    roomId, cursorId, PageRequest.of(0, size)
            );
        }

        return messages.stream().map(ChatMessageResponse::from).toList();
    }

    // ─────────────────────────────────────────────
    // 메시지 전송 (REST)
    // ─────────────────────────────────────────────
    @Transactional
    public ChatMessageResponse sendMessage(Long roomId, String email, String content) {
        ChatRoom room = requireParticipant(roomId, email);
        if (room.getStatus() != ChatRoom.Status.ACCEPTED) {
            throw new CustomException(GlobalErrorCode.CHAT_ROOM_NOT_ACTIVE);
        }
        // requireParticipant에서 fetch join으로 company/owner를 이미 로드했으므로
        // 별도 getUser() 조회 없이 room에서 sender를 꺼냄
        User sender = room.getCompany().getEmail().equals(email)
                ? room.getCompany() : room.getOwner();

        ChatMessage message = chatMessageRepository.save(
                ChatMessage.builder().room(room).sender(sender).content(content.trim()).build()
        );
        return ChatMessageResponse.from(message);
    }

    // ─────────────────────────────────────────────
    // 첨부파일 전송
    // ─────────────────────────────────────────────
    @Transactional
    public ChatMessageResponse sendAttachment(Long roomId, String email, MultipartFile file) {
        ChatRoom room = requireParticipant(roomId, email);
        if (room.getStatus() != ChatRoom.Status.ACCEPTED) {
            throw new CustomException(GlobalErrorCode.CHAT_ROOM_NOT_ACTIVE);
        }
        User sender = room.getCompany().getEmail().equals(email)
                ? room.getCompany() : room.getOwner();

        String attachmentPath = imageStorageService.storeDocument(file, "chat");
        String originalName = file.getOriginalFilename() != null
                ? file.getOriginalFilename() : attachmentPath;

        ChatMessage message = chatMessageRepository.save(
                ChatMessage.builder()
                        .room(room)
                        .sender(sender)
                        .content("")          // 첨부파일 메시지는 content 빈 문자열
                        .attachmentPath(attachmentPath)
                        .attachmentOriginalName(originalName)
                        .build()
        );
        return ChatMessageResponse.from(message);
    }

    // ─────────────────────────────────────────────
    // 채팅방 상태 변경
    // ─────────────────────────────────────────────
    @Transactional
    public ChatRoomResponse acceptRoom(Long roomId, String email) {
        ChatRoom room = requireParticipant(roomId, email);
        if (!room.getOwner().getEmail().equals(email)) {
            throw new CustomException(GlobalErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }
        if (room.getStatus() != ChatRoom.Status.PENDING) {
            throw new CustomException(GlobalErrorCode.CHAT_ROOM_NOT_ACTIVE);
        }
        room.accept();
        return toRoomResponse(room, email, null);
    }

    @Transactional
    public ChatRoomResponse rejectRoom(Long roomId, String email) {
        ChatRoom room = requireParticipant(roomId, email);
        if (!room.getOwner().getEmail().equals(email)) {
            throw new CustomException(GlobalErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }
        if (room.getStatus() != ChatRoom.Status.PENDING) {
            throw new CustomException(GlobalErrorCode.CHAT_ROOM_NOT_ACTIVE);
        }
        room.reject();
        return toRoomResponse(room, email, null);
    }

    @Transactional
    public void closeRoom(Long roomId, String email) {
        ChatRoom room = requireParticipant(roomId, email);
        if (room.getStatus() != ChatRoom.Status.ACCEPTED
                && room.getStatus() != ChatRoom.Status.PENDING) {
            throw new CustomException(GlobalErrorCode.CHAT_ROOM_NOT_ACTIVE);
        }
        room.close();
    }

    // ─────────────────────────────────────────────
    // WebSocket 구독 시 참여자 검증
    // ─────────────────────────────────────────────
    @Transactional(readOnly = true)
    public void validateParticipant(Long roomId, String email) {
        requireParticipant(roomId, email);
    }

    // ─────────────────────────────────────────────
    // 내부 헬퍼
    // ─────────────────────────────────────────────
    private ChatRoom requireParticipant(Long roomId, String email) {
        ChatRoom room = chatRoomRepository.findDetailById(roomId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.CHAT_ROOM_NOT_FOUND));
        if (!room.getCompany().getEmail().equals(email)
                && !room.getOwner().getEmail().equals(email)) {
            throw new CustomException(GlobalErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }
        return room;
    }

    private ChatRoomResponse toRoomResponse(ChatRoom room, String email, ChatMessage latestMessage) {
        ChatMessage latest = latestMessage != null
                ? latestMessage
                : chatMessageRepository.findTopByRoomIdOrderByIdDesc(room.getId()).orElse(null);
        long unread = chatMessageRepository.countUnreadByRoomIdAndReceiverEmail(room.getId(), email);
        return ChatRoomResponse.from(
                room, email, unread,
                latest == null ? null : latest.getContent(),
                latest == null ? null : latest.getCreatedAt()
        );
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.USER_NOT_FOUND));
    }
}
