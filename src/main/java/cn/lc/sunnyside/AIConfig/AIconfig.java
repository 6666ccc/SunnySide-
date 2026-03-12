package cn.lc.sunnyside.AIConfig;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AIconfig {
    @Bean
    public ChatMemory chatMemory() {
        // 使用内存存储库，不依赖任何数据库
        return MessageWindowChatMemory.builder().build();
    }
}
