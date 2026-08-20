package com.heilous.chat.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
@Getter @NoArgsConstructor
public class ChatMessageRequest {
    @NotBlank(message = "메시지 내용은 필수입니다.") @Size(max = 2000, message = "메시지는 2,000자 이하여야 합니다.")
    private String content;
}
