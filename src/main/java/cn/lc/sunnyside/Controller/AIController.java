package cn.lc.sunnyside.Controller;

import cn.lc.sunnyside.Auth.FamilyLoginContext;
import cn.lc.sunnyside.POJO.DTO.ChatReply;
import cn.lc.sunnyside.Service.AIService;
import cn.lc.sunnyside.Workflow.Health.HealthWorkflowService;
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
    private final HealthWorkflowService healthWorkflowService;

    public AIController(AIService aiService, HealthWorkflowService healthWorkflowService) {
        this.aiService = aiService;
        this.healthWorkflowService = healthWorkflowService;
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
        if (userId == null || userId.isBlank()) {
            return legacyUserId;
        }
        return userId;
    }

    private ChatReply.ChatReplyRecord userIdRequiredReply() {
        return new ChatReply.ChatReplyRecord("userId不能为空。", List.of());
    }

    /**
     * 体验家属健康查询专属工作流
     * 
     * @param query 用户输入，例如：“查一下我爸昨天的健康状况”
     * @param phone 手动传入的手机号（测试用），如果不传则尝试从登录上下文中获取
     * @return 工作流执行结果
     */
    @GetMapping("/api/workflow/health-chat")
    public String healthWorkflowChat(
            @RequestParam(name = "query", defaultValue = "查一下我爸昨天的健康状况") String query,
            @RequestParam(name = "phone", required = false) String phone) {

        String familyPhone = phone;
        // 如果未手动传入手机号，尝试从全局登录态中获取
        if (familyPhone == null || familyPhone.isBlank()) {
            familyPhone = FamilyLoginContext.get().map(ctx -> ctx.phone()).orElse(null);
        }

        return healthWorkflowService.executeWorkflow(query, familyPhone);
    }
}
