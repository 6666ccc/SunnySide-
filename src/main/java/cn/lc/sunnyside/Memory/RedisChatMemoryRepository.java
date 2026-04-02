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

    /**
     * JSON 反序列化目标类型：`[{type,text}, ...]`
     */
    private static final TypeReference<List<StoredMessage>> LIST_TYPE = new TypeReference<List<StoredMessage>>() {};

    /**
     * Redis 操作模板
     */
    private final StringRedisTemplate stringRedisTemplate;
    /**
     * 对象映射器
     */
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
        // 读失败/格式不对时：记忆功能降级为“无历史”，避免影响对话主链路
        List<StoredMessage> stored = readStored(key(conversationId));
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

        existing = trimToWindow(existing);
        writeStored(key, existing);
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
            String id = conversationIdFromKey(key);
            if (id != null) {
                ids.add(id);
            }
        }
        return Collections.unmodifiableList(ids);
    }

    private String key(String conversationId) {
        return keyPrefix + conversationId;
    }

    private List<StoredMessage> readStored(String key) {
        String json = (StringUtils.hasText(key) ? stringRedisTemplate.opsForValue().get(key) : null);
        if (!StringUtils.hasText(json)) {
            return new ArrayList<>();
        }
        try {
            List<StoredMessage> stored = objectMapper.readValue(json, LIST_TYPE);
            return stored != null ? new ArrayList<>(stored) : new ArrayList<>();
        } catch (Exception ex) {
            // 兼容：Redis 里可能残留旧格式/被人工写入非 JSON；此时不让异常干扰业务流程
            return new ArrayList<>();
        }
    }

    /**
     * 写入时使用 Sliding TTL：每次成功写入都会刷新过期时间。
     * 写失败时不抛出，避免“记忆模块”影响对话主流程。
     */
    private void writeStored(String key, List<StoredMessage> stored) {
        try {
            String json = objectMapper.writeValueAsString(stored != null ? stored : List.of());
            stringRedisTemplate.opsForValue().set(key, json, ttl);
        } catch (Exception ex) {
            // 写入失败：记忆功能退化为“本轮不持久化”
        }
    }

    private List<StoredMessage> trimToWindow(List<StoredMessage> stored) {
        if (stored == null || stored.isEmpty()) {
            return new ArrayList<>();
        }
        if (maxMessages <= 0 || stored.size() <= maxMessages) {
            return stored;
        }
        // 只保留最后 N 条，避免 key 过大且控制 prompt 上下文长度
        return stored.subList(stored.size() - maxMessages, stored.size());
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
        text = (text != null ? text : "");
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

        if (mt != null) {
            switch (mt) {
                case USER:
                    return new UserMessage(text);
                case ASSISTANT:
                    return new AssistantMessage(text);
                case SYSTEM:
                    return new SystemMessage(text);
                default:
                    break;
            }
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

    private String conversationIdFromKey(String key) {
        if (!StringUtils.hasText(key)) {
            return null;
        }
        if (keyPrefix.isEmpty()) {
            return key;
        }
        if (key.startsWith(keyPrefix)) {
            return key.substring(keyPrefix.length());
        }
        return null;
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
