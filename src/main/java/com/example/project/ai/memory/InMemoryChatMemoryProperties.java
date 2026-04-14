package com.example.project.ai.memory;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 内存会话记忆（{@link org.springframework.ai.chat.memory.MessageWindowChatMemory}）的可选配置。
 * <p>
 * 与 Spring AI 文档中的窗口记忆一致：超出条数时淘汰较早消息，并保留系统消息策略。
 */
@ConfigurationProperties(prefix = "app.ai.chat-memory")
public class InMemoryChatMemoryProperties {

    /**
     * 每个会话保留的最大消息条数（含多轮往返），默认与 Spring AI 文档一致为 20。
     */
    private int maxMessages = 20;

    public int getMaxMessages() {
        return maxMessages;
    }

    public void setMaxMessages(int maxMessages) {
        this.maxMessages = maxMessages;
    }
}
