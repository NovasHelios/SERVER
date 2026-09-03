package com.heilous.chat.controller;

import com.heilous.chat.dto.*;
import com.heilous.chat.service.ChatService;
import com.heilous.common.dto.APIResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    @PostMapping public APIResponse<ChatRoomResponse> create(@Valid @RequestBody CreateChatRoomRequest request, @AuthenticationPrincipal String email) { return APIResponse.ok(chatService.createRoom(request.getLandId(), request.getInitialMessage(), email)); }
    @GetMapping public APIResponse<List<ChatRoomResponse>> list(@AuthenticationPrincipal String email) { return APIResponse.ok(chatService.getRooms(email)); }
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
    @PatchMapping("/{roomId}/accept") public APIResponse<ChatRoomResponse> accept(@PathVariable Long roomId, @AuthenticationPrincipal String email) { return APIResponse.ok(chatService.acceptRoom(roomId, email)); }
    @PatchMapping("/{roomId}/reject") public APIResponse<ChatRoomResponse> reject(@PathVariable Long roomId, @AuthenticationPrincipal String email) { return APIResponse.ok(chatService.rejectRoom(roomId, email)); }
    @PatchMapping("/{roomId}/close") public APIResponse<String> close(@PathVariable Long roomId, @AuthenticationPrincipal String email) { chatService.closeRoom(roomId, email); return APIResponse.ok("상담 채팅방이 종료되었습니다."); }
}
