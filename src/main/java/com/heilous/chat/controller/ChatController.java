package com.heilous.chat.controller;

import com.heilous.chat.dto.*;
import com.heilous.chat.service.ChatService;
import com.heilous.common.dto.APIResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@RestController
@CrossOrigin
@RequestMapping("/api/chat/rooms")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class ChatController {
    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;
    @Operation(
            summary = "채팅방 생성",
            description = "COMPANY 사용자가 특정 토지에 대해 채팅 상담을 요청합니다. landId와 첫 메시지를 함께 전송하면 채팅방이 생성되며 토지 소유자에게 요청이 전달됩니다."
    )
    @PostMapping public APIResponse<ChatRoomResponse> create(@Valid @RequestBody CreateChatRoomRequest request, @AuthenticationPrincipal String email) { return APIResponse.ok(chatService.createRoom(request.getLandId(), request.getInitialMessage(), email)); }
    @Operation(
            summary = "내 채팅방 목록 조회",
            description = "로그인한 사용자가 참여 중인 모든 채팅방 목록을 조회합니다. USER는 본인 토지에 대한 채팅방, COMPANY는 본인이 요청한 채팅방이 반환됩니다."
    )
    @GetMapping public APIResponse<List<ChatRoomResponse>> list(@AuthenticationPrincipal String email) { return APIResponse.ok(chatService.getRooms(email)); }
    @Operation(
            summary = "채팅 메시지 목록 조회",
            description = "특정 채팅방의 전체 메시지 이력을 조회합니다. 해당 채팅방의 참여자만 조회할 수 있습니다."
    )
    @GetMapping("/{roomId}/messages") public APIResponse<List<ChatMessageResponse>> messages(@PathVariable Long roomId, @AuthenticationPrincipal String email) { return APIResponse.ok(chatService.getMessages(roomId, email)); }
    @PostMapping("/{roomId}/messages") public APIResponse<ChatMessageResponse> sendMessage(@PathVariable Long roomId, @Valid @RequestBody ChatMessageRequest request, @AuthenticationPrincipal String email) {
        ChatMessageResponse response = chatService.sendMessage(roomId, email, request.getContent());
        messagingTemplate.convertAndSend("/topic/chat/rooms/" + roomId, response);
        return APIResponse.ok(response);
    }
    @PostMapping(value = "/{roomId}/attachments", consumes = "multipart/form-data") public APIResponse<ChatMessageResponse> attachment(@PathVariable Long roomId, @RequestPart("file") MultipartFile file, @AuthenticationPrincipal String email) {
        ChatMessageResponse response = chatService.sendAttachment(roomId, email, file);
        messagingTemplate.convertAndSend("/topic/chat/rooms/" + roomId, response);
        return APIResponse.ok(response);
    }
    @Operation(
            summary = "채팅 상담 요청 수락",
            description = "토지 소유자(USER)가 기업의 채팅 상담 요청을 수락합니다. 수락 후 양측이 자유롭게 메시지를 주고받을 수 있습니다."
    )
    @PatchMapping("/{roomId}/accept") public APIResponse<ChatRoomResponse> accept(@PathVariable Long roomId, @AuthenticationPrincipal String email) { return APIResponse.ok(chatService.acceptRoom(roomId, email)); }
    @Operation(
            summary = "채팅 상담 요청 거절",
            description = "토지 소유자(USER)가 기업의 채팅 상담 요청을 거절합니다. 거절된 채팅방은 더 이상 메시지를 주고받을 수 없습니다."
    )
    @PatchMapping("/{roomId}/reject") public APIResponse<ChatRoomResponse> reject(@PathVariable Long roomId, @AuthenticationPrincipal String email) { return APIResponse.ok(chatService.rejectRoom(roomId, email)); }
    @Operation(
            summary = "채팅방 종료",
            description = "채팅방 참여자가 상담을 종료합니다. 종료된 채팅방은 메시지 전송이 불가능하며 이력은 유지됩니다."
    )
    @PatchMapping("/{roomId}/close") public APIResponse<String> close(@PathVariable Long roomId, @AuthenticationPrincipal String email) { chatService.closeRoom(roomId, email); return APIResponse.ok("상담 채팅방이 종료되었습니다."); }
}
