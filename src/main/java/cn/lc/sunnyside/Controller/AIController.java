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

    /**
     * 构造 AI 对话控制器，注入核心对话服务与健康工作流服务。
     *
     * @param aiService             AI 对话与多模态能力服务
     * @param healthWorkflowService 家属健康查询工作流服务
     */
    public AIController(AIService aiService, HealthWorkflowService healthWorkflowService) {
        this.aiService = aiService;
        this.healthWorkflowService = healthWorkflowService;
    }

    /**
     * 老人端单轮对话接口。
     * 接收用户输入与会话ID，转发到老人端对话服务并返回结构化回复。
     *
     * @param userInput    用户文本输入
     * @param userId       当前推荐的会话ID参数名
     * @param legacyUserId 兼容旧版会话ID参数名
     * @return 模型回复与工具调用记录
     */
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
        String conversationId = resolveConversationId(userId, legacyUserId);
        if (conversationId == null || conversationId.isBlank()) {
            return userIdRequiredReply();
        }
        return aiService.relativesChat(userInput, conversationId);
    }

    /**
     * 流式对话接口。
     * 通过 SSE 逐段输出模型回复，适合前端打字机效果展示。
     *
     * @param userInput    用户文本输入
     * @param userId       当前推荐的会话ID参数名
     * @param legacyUserId 兼容旧版会话ID参数名
     * @return 流式回复内容
     */
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

    /**
     * 多模态对话接口。
     * 支持文本配合图片、音频、视频文件进行联合提问。
     *
     * @param userInput    用户文本输入
     * @param mediaFiles   上传的多媒体文件集合
     * @param userId       当前推荐的会话ID参数名
     * @param legacyUserId 兼容旧版会话ID参数名
     * @return 模型回复与工具调用记录
     */
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

    /**
     * 统一解析会话ID。
     * 优先使用 userId，若为空则回退 legacyUserId。
     *
     * @param userId       新参数名
     * @param legacyUserId 旧参数名
     * @return 解析后的会话ID
     */
    private String resolveConversationId(String userId, String legacyUserId) {
        if (userId == null || userId.isBlank()) {
            return legacyUserId;
        }
        return userId;
    }

    /**
     * 构造缺少会话ID时的统一错误响应。
     *
     * @return 固定错误文案的对话回复对象
     */
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
        if (familyPhone == null || familyPhone.isBlank()) {
            familyPhone = FamilyLoginContext.get().map(ctx -> ctx.phone()).orElse(null);
        }

        return healthWorkflowService.executeWorkflow(query, familyPhone);
    }
}
