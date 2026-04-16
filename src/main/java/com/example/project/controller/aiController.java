package com.example.project.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.example.project.ai.client.AIChat;
import com.example.project.service.ChatMemoryConversationService;
import com.example.project.service.relativeService;

import reactor.core.Disposable;

@RestController
public class aiController {
    @Autowired
    private AIChat aiChat;

    @Autowired
    private relativeService relativeService;

    @Autowired
    private ChatMemoryConversationService chatMemoryConversationService;

    private static final long CHAT_SSE_TIMEOUT_MS = 600_000L;

    /**
     * 聊天接口（SSE 流式：每个 data 事件为一段模型输出文本）
     */
    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestAttribute("userId") String jwtSubject, @RequestParam("timeId") String timeId,
            @RequestParam("message") String message) {
        Long userId = relativeService.resolveRelativeUserId(jwtSubject);
        SseEmitter emitter = new SseEmitter(CHAT_SSE_TIMEOUT_MS);
        MediaType textUtf8 = new MediaType(MediaType.TEXT_PLAIN, StandardCharsets.UTF_8);

        AtomicReference<Disposable> subscriptionRef = new AtomicReference<>();
        Disposable subscription = aiChat.stream(userId, timeId, message).subscribe(chunk -> {
            try {
                emitter.send(SseEmitter.event().data(chunk, textUtf8));
            } catch (IOException e) {
                Disposable d = subscriptionRef.get();
                if (d != null) {
                    d.dispose();
                }
                emitter.completeWithError(e);
            }
        }, emitter::completeWithError, emitter::complete);
        subscriptionRef.set(subscription);

        emitter.onCompletion(subscription::dispose);
        emitter.onTimeout(() -> {
            subscription.dispose();
            emitter.complete();
        });

        return emitter;
    }

    /**
     * 删除指定会话在服务端持久化的聊天记忆（与 {@code timeId} 对应表字段 {@code conversation_id}）。
     */
    @DeleteMapping("/chat/memory")
    public ResponseEntity<Void> deleteChatMemory(@RequestParam("timeId") String timeId) {
        if (!StringUtils.hasText(timeId)) {
            return ResponseEntity.badRequest().build();
        }
        chatMemoryConversationService.deleteByConversationId(timeId);
        return ResponseEntity.noContent().build();
    }
}
