package com.heilous.global.config;

import com.heilous.global.auth.JwtProvider;
import com.heilous.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class ChatWebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final JwtProvider jwtProvider;
    private final ChatService chatService;
    @Override public void configureMessageBroker(MessageBrokerRegistry registry) { registry.enableSimpleBroker("/topic"); registry.setApplicationDestinationPrefixes("/app"); }
    @Override public void registerStompEndpoints(StompEndpointRegistry registry) { registry.addEndpoint("/ws/chat").setAllowedOriginPatterns("*"); }
    @Override public void configureClientInboundChannel(ChannelRegistration registration) { registration.interceptors(new JwtStompInterceptor(jwtProvider, chatService)); }

    @RequiredArgsConstructor
    static class JwtStompInterceptor implements ChannelInterceptor {
        private final JwtProvider jwtProvider;
        private final ChatService chatService;
        @Override public Message<?> preSend(Message<?> message, MessageChannel channel) {
            StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
            if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                String authorization = accessor.getFirstNativeHeader("Authorization");
                if (authorization == null || !authorization.startsWith("Bearer ") || !jwtProvider.validateToken(authorization.substring(7))) throw new IllegalArgumentException("유효한 JWT가 필요합니다.");
                String token = authorization.substring(7);
                accessor.setUser(new UsernamePasswordAuthenticationToken(jwtProvider.getEmail(token), null, List.of(new SimpleGrantedAuthority("ROLE_" + jwtProvider.getRole(token)))));
            }
            if (accessor != null && !StompCommand.CONNECT.equals(accessor.getCommand()) && accessor.getUser() == null) {
                throw new IllegalArgumentException("인증된 WebSocket 연결이 필요합니다.");
            }
            if (accessor != null && StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                String destination = accessor.getDestination();
                if (destination != null && destination.matches("/topic/chat/rooms/\\d+")) {
                    Long roomId = Long.valueOf(destination.substring(destination.lastIndexOf('/') + 1));
                    chatService.validateParticipant(roomId, accessor.getUser().getName());
                }
            }
            return message;
        }
    }
}
