package com.heilous.chat.controller;

import com.heilous.chat.dto.ChatMessageRequest;
import com.heilous.chat.dto.ChatMessageResponse;
import com.heilous.chat.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {
    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;
    @MessageMapping("/chat/rooms/{roomId}/messages")
    public void send(@DestinationVariable Long roomId, @Valid ChatMessageRequest request, Principal principal) {
        ChatMessageResponse response = chatService.sendMessage(roomId, principal.getName(), request.getContent());
        messagingTemplate.convertAndSend("/topic/chat/rooms/" + roomId, response);
    }
}
