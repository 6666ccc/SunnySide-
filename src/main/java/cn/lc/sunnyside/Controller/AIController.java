package cn.lc.sunnyside.Controller;

import cn.lc.sunnyside.POJO.DTO.ChatReply;
import cn.lc.sunnyside.Service.AIService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
public class AIController {
    private final AIService aiService;

    public AIController(AIService aiService) {
        this.aiService = aiService;
    }

    @GetMapping("/ElderChat")
    public ChatReply.ChatReplyRecord ElderChat(
            @RequestParam("userInput") String userInput,
            @RequestParam(value = "userId", required = false) String userId,
            @RequestParam(value = "UserID", required = false) String legacyUserId) {
        String conversationId = resolveConversationId(userId, legacyUserId);
        if (conversationId == null || conversationId.isBlank()) {
            return userIdRequiredReply();
        }
        return aiService.elderChat(userInput, conversationId);
    }


    /**
     * 家属端对话接口：根据用户输入及会话ID返回单次回复
     * 支持 userId / UserID 双写兼容，优先取 userId
     */
    @GetMapping("/RelativesChat")
    public ChatReply.ChatReplyRecord RelativesChat(
            @RequestParam("userInput") String userInput,
            @RequestParam(value = "userId", required = false) String userId,
            @RequestParam(value = "UserID", required = false) String legacyUserId) {

        // 解析会话ID，优先取 userId，若为空则取 UserID
        String conversationId = resolveConversationId(userId, legacyUserId);
        // 检查会话ID是否为空
        if (conversationId == null || conversationId.isBlank()) {
            return userIdRequiredReply();
        }
        return aiService.relativesChat(userInput, conversationId);
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(
            @RequestParam("userInput") String userInput,
            @RequestParam(value = "userId", required = false) String userId,
            @RequestParam(value = "UserID", required = false) String legacyUserId) {
        String conversationId = resolveConversationId(userId, legacyUserId);
        if (conversationId == null || conversationId.isBlank()) {
            return Flux.just("userId不能为空。");
        }
        return aiService.streamChat(userInput, conversationId);
    }

    @PostMapping(value = "/chat/multimodal", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ChatReply.ChatReplyRecord multimodalChat(
            @RequestParam("userInput") String userInput,
            @RequestParam("media") List<MultipartFile> mediaFiles,
            @RequestParam(value = "userId", required = false) String userId,
            @RequestParam(value = "UserID", required = false) String legacyUserId) {
        String conversationId = resolveConversationId(userId, legacyUserId);
        if (conversationId == null || conversationId.isBlank()) {
            return userIdRequiredReply();
        }
        return aiService.multimodalChat(userInput, mediaFiles, conversationId);
    }

    private String resolveConversationId(String userId, String legacyUserId) {
        return (userId == null || userId.isBlank()) ? legacyUserId : userId;
    }

    private ChatReply.ChatReplyRecord userIdRequiredReply() {
        return new ChatReply.ChatReplyRecord("userId不能为空。", List.of());
    }
}
