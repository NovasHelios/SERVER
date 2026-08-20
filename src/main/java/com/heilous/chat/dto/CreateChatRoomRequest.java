package com.heilous.chat.dto;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
@Getter @NoArgsConstructor
public class CreateChatRoomRequest {
    @NotNull private Long landId;
    @jakarta.validation.constraints.Size(max = 2000, message = "상담 요청 메시지는 2,000자 이하여야 합니다.")
    private String initialMessage;
}
