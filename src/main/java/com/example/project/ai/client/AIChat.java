package com.example.project.ai.client;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import com.example.project.ai.prompt.MedicalSystemPromptTemplate;
import com.example.project.ai.tools.AITool;

@Component
public class AIChat {

    @Autowired
    private AITool aiTool;

    @Autowired
    private MedicalSystemPromptTemplate medicalSystemPromptTemplate;

    private final ChatClient chatClient;

    public AIChat(ChatClient.Builder chatClient, Advisor retrievalAugmentationAdvisor,
            MessageChatMemoryAdvisor messageChatMemoryAdvisor) {
        this.chatClient = chatClient
                .defaultAdvisors(retrievalAugmentationAdvisor, messageChatMemoryAdvisor)
                .build();
    }

    public Flux<String> stream(Long userId, String timeId, String message) {
        var parts = medicalSystemPromptTemplate.renderParts(userId, null);
        String userTurn = parts.combinedUserMessage(message);
        return chatClient.prompt()
                .system(parts.systemPrompt())
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, timeId))
                .tools(aiTool)
                .user(userTurn)
                .stream()
                .content();
    }
}
