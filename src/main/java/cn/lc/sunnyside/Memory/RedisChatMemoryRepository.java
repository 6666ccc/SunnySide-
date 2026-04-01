package cn.lc.sunnyside.Memory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 基础 Redis 持久化 ChatMemoryRepository：
 * - Key: {keyPrefix}{conversationId}
 * - Value: JSON 数组（按时间顺序 oldest->newest）
 * - Sliding TTL: 每次写入刷新 TTL
 * - Window: 保留最近 maxMessages 条
 */
public class RedisChatMemoryRepository implements ChatMemoryRepository {

    private static final TypeReference<List<StoredMessage>> LIST_TYPE = new TypeReference<List<StoredMessage>>() {};

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final String keyPrefix;
    private final Duration ttl;
    private final int maxMessages;

    public RedisChatMemoryRepository(StringRedisTemplate stringRedisTemplate,
                                    ObjectMapper objectMapper,
                                    String keyPrefix,
                                    Duration ttl,
                                    int maxMessages) {
        this.stringRedisTemplate = Objects.requireNonNull(stringRedisTemplate, "stringRedisTemplate");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.keyPrefix = (keyPrefix != null ? keyPrefix : "");
        this.ttl = Objects.requireNonNull(ttl, "ttl");
        this.maxMessages = maxMessages;
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        String key = key(conversationId);
        String json = stringRedisTemplate.opsForValue().get(key);
        if (!StringUtils.hasText(json)) {
            return List.of();
        }

        List<StoredMessage> stored;
        try {
            stored = objectMapper.readValue(json, LIST_TYPE);
        } catch (Exception ex) {
            // 反序列化失败时，为避免影响对话，直接视为无记忆
            return List.of();
        }
        if (stored == null || stored.isEmpty()) {
            return List.of();
        }

        List<Message> out = new ArrayList<>(stored.size());
        for (StoredMessage sm : stored) {
            Message m = toMessage(sm);
            if (m != null) {
                out.add(m);
            }
        }
        return Collections.unmodifiableList(out);
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        if (!StringUtils.hasText(conversationId) || messages == null || messages.isEmpty()) {
            return;
        }

        String key = key(conversationId);
        List<StoredMessage> existing = readStored(key);

        for (Message m : messages) {
            if (m == null) {
                continue;
            }
            StoredMessage sm = fromMessage(m);
            if (sm != null) {
                existing.add(sm);
            }
        }

        if (maxMessages > 0 && existing.size() > maxMessages) {
            existing = existing.subList(existing.size() - maxMessages, existing.size());
        }

        try {
            String json = objectMapper.writeValueAsString(existing);
            stringRedisTemplate.opsForValue().set(key, json, ttl);
        } catch (Exception ex) {
            // 写入失败时不抛出，避免影响主链路；记忆功能将退化为本轮不持久化
        }
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        if (!StringUtils.hasText(conversationId)) {
            return;
        }
        stringRedisTemplate.delete(key(conversationId));
    }

    @Override
    public List<String> findConversationIds() {
        String pattern = keyPrefix + "*";
        Set<String> keys = stringRedisTemplate.keys(pattern);
        if (keys == null || keys.isEmpty()) {
            return List.of();
        }
        List<String> ids = new ArrayList<>(keys.size());
        for (String key : keys) {
            if (!StringUtils.hasText(key)) {
                continue;
            }
            if (keyPrefix.isEmpty()) {
                ids.add(key);
            } else if (key.startsWith(keyPrefix)) {
                ids.add(key.substring(keyPrefix.length()));
            }
        }
        return Collections.unmodifiableList(ids);
    }

    private String key(String conversationId) {
        return keyPrefix + conversationId;
    }

    private List<StoredMessage> readStored(String key) {
        String json = stringRedisTemplate.opsForValue().get(key);
        if (!StringUtils.hasText(json)) {
            return new ArrayList<>();
        }
        try {
            List<StoredMessage> stored = objectMapper.readValue(json, LIST_TYPE);
            return stored != null ? new ArrayList<>(stored) : new ArrayList<>();
        } catch (Exception ex) {
            return new ArrayList<>();
        }
    }

    private StoredMessage fromMessage(Message message) {
        if (message == null) {
            return null;
        }
        String type = null;
        try {
            MessageType mt = message.getMessageType();
            type = (mt != null ? mt.name() : null);
        } catch (Exception ignored) {
        }
        String text = extractText(message);
        if (!StringUtils.hasText(text)) {
            text = "";
        }
        if (!StringUtils.hasText(type)) {
            type = "UNKNOWN";
        }
        return new StoredMessage(type, text);
    }

    private Message toMessage(StoredMessage sm) {
        if (sm == null) {
            return null;
        }
        String type = sm.getType();
        String text = sm.getText() != null ? sm.getText() : "";
        MessageType mt = parseType(type);

        if (mt == MessageType.USER) {
            return new UserMessage(text);
        }
        if (mt == MessageType.ASSISTANT) {
            return new AssistantMessage(text);
        }
        if (mt == MessageType.SYSTEM) {
            return new SystemMessage(text);
        }
        // 对于未知类型，按 assistant 降级处理（尽量不丢内容）
        return new AssistantMessage(text);
    }

    private MessageType parseType(String type) {
        if (!StringUtils.hasText(type)) {
            return null;
        }
        try {
            return MessageType.valueOf(type.trim());
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * Spring AI 不同版本里消息文本字段可能叫 getContent / getText。
     * 这里用反射做一个兼容提取，避免编译期绑定到某一个方法名。
     */
    private String extractText(Message message) {
        String v = invokeStringNoArg(message, "getText");
        if (StringUtils.hasText(v)) {
            return v;
        }
        v = invokeStringNoArg(message, "getContent");
        if (StringUtils.hasText(v)) {
            return v;
        }
        return "";
    }

    private String invokeStringNoArg(Object target, String methodName) {
        try {
            Method m = target.getClass().getMethod(methodName);
            Object v = m.invoke(target);
            return v != null ? v.toString() : null;
        } catch (Exception ex) {
            return null;
        }
    }
}
