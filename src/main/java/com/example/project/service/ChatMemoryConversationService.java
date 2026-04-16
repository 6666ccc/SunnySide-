package com.example.project.service;

import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.stereotype.Service;

@Service
public class ChatMemoryConversationService {

    private final ChatMemoryRepository chatMemoryRepository;

    public ChatMemoryConversationService(ChatMemoryRepository chatMemoryRepository) {
        this.chatMemoryRepository = chatMemoryRepository;
    }

    /**
     * 按会话 ID 删除 JDBC 表 {@code spring_ai_chat_memory} 中该会话的全部消息（与
     * {@link org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor} 使用的
     * {@link org.springframework.ai.chat.memory.ChatMemory#CONVERSATION_ID} 一致）。
     */
    public void deleteByConversationId(String conversationId) {
        chatMemoryRepository.deleteByConversationId(conversationId.trim());
    }
}
