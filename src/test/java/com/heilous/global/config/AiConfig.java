package com.heilous.global.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    /**
     * ChatClient Bean 등록.
     * temperature=0 은 application.yml의 spring.ai.openai.chat.options.temperature=0.0 으로 적용됩니다.
     * 동일한 프롬프트에 대해 항상 동일한 답변이 생성됩니다.
     */
    @Bean
    public ChatClient chatClient(OpenAiChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }
}
