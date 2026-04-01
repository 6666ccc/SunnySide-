package cn.lc.sunnyside.Memory;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.ai.chat.memory")
public class ChatMemoryProperties {

    /**
     * MessageWindowChatMemory 的窗口大小（最近 N 条消息）。
     */
    private int maxMessages = 10;

    private final Redis redis = new Redis();

    public int getMaxMessages() {
        return maxMessages;
    }

    public void setMaxMessages(int maxMessages) {
        this.maxMessages = maxMessages;
    }

    public Redis getRedis() {
        return redis;
    }

    public static class Redis {
        /**
         * Redis key 前缀。最终 key 形如：{keyPrefix}{conversationId}
         */
        private String keyPrefix = "app:ai:chat-memory:";

        /**
         * TTL 秒数；每次写入都会刷新（滑动过期）。
         */
        private long ttlSeconds = 86400;

        public String getKeyPrefix() {
            return keyPrefix;
        }

        public void setKeyPrefix(String keyPrefix) {
            this.keyPrefix = keyPrefix;
        }

        public long getTtlSeconds() {
            return ttlSeconds;
        }

        public void setTtlSeconds(long ttlSeconds) {
            this.ttlSeconds = ttlSeconds;
        }
    }
}
