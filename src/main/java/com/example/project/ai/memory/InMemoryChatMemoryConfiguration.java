package com.example.project.ai.memory;

import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 基于 {@link InMemoryChatMemoryRepository} 的内存会话存储，配合 {@link MessageWindowChatMemory} 做窗口裁剪。
 * <p>
 * 若 classpath 上已存在其他 {@link ChatMemoryRepository}（如 JDBC/Cassandra）的自动配置，
 * 则本配置不会注册仓库与 {@link ChatMemory}，避免覆盖持久化实现。
 * <p>
 * 与 ChatClient 联用时，将 {@link MessageChatMemoryAdvisor} 加入
 * {@link org.springframework.ai.chat.client.ChatClient.Builder#defaultAdvisors(org.springframework.ai.chat.client.advisor.api.Advisor...)}，
 * 并在请求侧设置 {@link org.springframework.ai.chat.memory.ChatMemory#CONVERSATION_ID}。
 *
 * @see <a href="https://docs.spring.io/spring-ai/reference/api/chat-memory.html">Spring AI Chat Memory</a>
 */
@Configuration
@EnableConfigurationProperties(InMemoryChatMemoryProperties.class)
public class InMemoryChatMemoryConfiguration {

    @Bean
    @ConditionalOnMissingBean(ChatMemoryRepository.class)
    public InMemoryChatMemoryRepository inMemoryChatMemoryRepository() {
        return new InMemoryChatMemoryRepository();
    }

    @Bean
    @ConditionalOnMissingBean(ChatMemory.class)
    public ChatMemory chatMemory(ChatMemoryRepository chatMemoryRepository,
            InMemoryChatMemoryProperties properties) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(properties.getMaxMessages())
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(MessageChatMemoryAdvisor.class)
    public MessageChatMemoryAdvisor messageChatMemoryAdvisor(ChatMemory chatMemory) {
        return MessageChatMemoryAdvisor.builder(chatMemory).build();
    }
}
