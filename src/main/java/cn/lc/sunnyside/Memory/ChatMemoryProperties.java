package cn.lc.sunnyside.Memory;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.ai.chat.memory")
public class ChatMemoryProperties {

    /**
     * MessageWindowChatMemory 的窗口大小（最近 N 条消息）。
     */
    private int maxMessages = 10;

    private final Redis redis = new Redis();

    @Data
    public static class Redis {
        /**
         * Redis key 前缀。最终 key 形如：{keyPrefix}{conversationId}
         */
        private String keyPrefix = "app:ai:chat-memory:";

        /**
         * TTL 秒数；每次写入都会刷新（滑动过期）。
         */
        private long ttlSeconds = 86400;
    }
}