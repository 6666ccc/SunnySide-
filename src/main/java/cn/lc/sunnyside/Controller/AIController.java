package cn.lc.sunnyside.Controller;

import cn.lc.sunnyside.Auth.RelativeLoginContext;
import cn.lc.sunnyside.POJO.DTO.ChatReply;
import cn.lc.sunnyside.Service.AIService;
import cn.lc.sunnyside.Service.MedicalChatOrchestratorService;
import cn.lc.sunnyside.Service.medical.MedicalChatMode;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Locale;

@RestController
public class AIController {

    private final AIService aiService;

    private final MedicalChatOrchestratorService medicalChatOrchestratorService;

    public AIController(AIService aiService, MedicalChatOrchestratorService medicalChatOrchestratorService) {
        this.aiService = aiService;
        this.medicalChatOrchestratorService = medicalChatOrchestratorService;
    }

    /**
     * 患者和家属的对话接口，要求必须提供UserID参数以区分不同用户的对话上下文。
     * @param userInput
     * @param legacyUserId
     * @return
     */
    @GetMapping("/PatientChat")
    public ChatReply.ChatReplyRecord patientChat(
            @RequestParam("userInput") String userInput,
            @RequestParam(value = "UserID", required = false) String legacyUserId) {
        if (legacyUserId == null || legacyUserId.isBlank()) {
            return userIdRequiredReply();
        }


        return aiService.patientChat(userInput, legacyUserId);
    }

    /**
     * 家属之间的对话接口，要求必须提供UserID参数以区分不同用户的对话上下文。
     * @param userInput
     * @param legacyUserId
     * @return
     */
    @GetMapping("/RelativesChat")
    public ChatReply.ChatReplyRecord relativesChat(
            @RequestParam("userInput") String userInput,
            @RequestParam(value = "UserID", required = false) String legacyUserId) {
        if (legacyUserId == null || legacyUserId.isBlank()) {
            return userIdRequiredReply();
        }

        return aiService.relativesChat(userInput, legacyUserId);
    }

    /**
     * 流式对话接口，要求必须提供UserID参数以区分不同用户的对话上下文。
     * @param userInput
     * @param legacyUserId
     * @return
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(
            @RequestParam("userInput") String userInput,
            @RequestParam(value = "UserID", required = false) String legacyUserId) {
        if (legacyUserId == null || legacyUserId.isBlank()) {
            return Flux.just("userId不能为空。");
        }
        return aiService.streamChat(userInput, legacyUserId);
    }

    /**
     * 多模态对话接口，要求必须提供UserID参数以区分不同用户的对话上下文。(该接口纯多模态练手操作,很少使用)
     * @param userInput
     * @param mediaFiles
     * @param legacyUserId
     * @return
     */
    @PostMapping(value = "/chat/multimodal", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ChatReply.ChatReplyRecord multimodalChat(
            @RequestParam("userInput") String userInput,
            @RequestParam("media") List<MultipartFile> mediaFiles,
            @RequestParam(value = "UserID", required = false) String legacyUserId) {
        if (legacyUserId == null || legacyUserId.isBlank()) {
            return userIdRequiredReply();
        }
        return aiService.multimodalChat(userInput, mediaFiles, legacyUserId);
    }

    private ChatReply.ChatReplyRecord userIdRequiredReply() {
        return new ChatReply.ChatReplyRecord("userId不能为空。", List.of());
    }

    /**
     * 医疗工作流对话接口：通过自然语言查询体征、诊疗计划、医嘱等，由工作流路由并调用院内工具。
     * 与对话类接口一致，优先从 Bearer Token（亲属 JWT）解析登录态并关联患者；可选 query 参数 phone 可覆盖为指定亲属手机号（测试或代查场景）。
     * 登录且亲属仅有唯一默认患者时，无需在问题中写患者ID；多患者关联时需在说清姓名等问题中区分。
     * 示例：请求头携带亲属 JWT（Authorization: Bearer …），GET /api/workflow/medical-chat?query=查一下最新生命体征
     */
    @GetMapping("/api/workflow/medical-chat")
    @Deprecated
    public String medicalWorkflowChat(
            @RequestParam(name = "query", defaultValue = "查一下最新生命体征") String query,
            @RequestParam(name = "phone", required = false) String phone) {

        // 从请求头或参数中获取亲属手机号
        String relativePhone = phone != null && !phone.isBlank() ? phone.trim()
                : RelativeLoginContext.get().map(RelativeLoginContext::phone).orElse(null);

        // 旧实现（保留用于学习/灰度对比）：直接调用 workflow 图。
        // return agentRouterWorkflowService.executeWorkflow(query, relativePhone, null);

        // 新实现：转发到编排器，固定 mode 为 WORKFLOW。
        return medicalChatOrchestratorService.execute(query, relativePhone, null, MedicalChatMode.WORKFLOW);
    }


    /**
     * ReactAgent 显式 Loop：Graph 上 Model/Tool 多轮推理与工具调用，带 ModelCallLimitHook 迭代上限与 MemorySaver（按 UserID 作为 threadId）。
     * 主要与普通的workflow聊天接口进行灰度对比测试使用,目前未启用
     */
    @GetMapping("/api/agent/medical-chat")
    @Deprecated
    public String agentMedicalChat(
            @RequestParam(name = "query", defaultValue = "查一下最新生命体征") String query,
            @RequestParam(name = "phone", required = false) String phone,
            @RequestParam(value = "UserID", required = false) String userId) {
        // 从请求头或参数中获取亲属手机号
        String relativePhone = phone != null && !phone.isBlank() ? phone.trim()
                : RelativeLoginContext.get().map(RelativeLoginContext::phone).orElse(null);

        // 旧实现（保留用于学习/灰度对比）：显式校验并直接调用 ReactAgent Loop。
        // if (agentLoopMedicalService == null) { ... }
        // if (userId == null || userId.isBlank()) { ... }
        // return agentLoopMedicalService.execute(query, relativePhone, userId);

        // 新实现：转发到编排器，固定 mode 为 LOOP。
        return medicalChatOrchestratorService.execute(query, relativePhone, userId, MedicalChatMode.LOOP);
    }

    /**
     * 医疗对话统一入口（推荐）：默认 auto 优先 Agent Loop，必要时 fallback 到 Workflow，再不行回退到普通 ChatClient 链路。
     *
     * 参数说明：
     * - `phone`：可选；未登录时通过该参数提供亲属手机号。
     * - `UserID`：可选；用于会话隔离（Agent Loop threadId / Workflow runnable threadId）。
     * - `mode`：auto|loop|workflow（默认 auto）。
     */
    @GetMapping("/api/medical-chat")
    public String medicalChat(
            @RequestParam(name = "query", defaultValue = "查一下最新生命体征") String query,
            @RequestParam(name = "phone", required = false) String phone,
            @RequestParam(value = "UserID", required = false) String userId,
            @RequestParam(name = "mode", defaultValue = "auto") String mode) {

        // 从请求头或参数中获取亲属手机号
        String relativePhone = phone != null && !phone.isBlank() ? phone.trim()
                : RelativeLoginContext.get().map(RelativeLoginContext::phone).orElse(null);

        MedicalChatMode parsedMode = parseMedicalChatMode(mode);
        return medicalChatOrchestratorService.execute(query, relativePhone, userId, parsedMode);
    }

    private MedicalChatMode parseMedicalChatMode(String mode) {
        if (mode == null || mode.isBlank()) {
            return MedicalChatMode.AUTO;
        }
        String m = mode.trim().toLowerCase(Locale.ROOT);
        return switch (m) {
            case "loop" -> MedicalChatMode.LOOP;
            case "workflow" -> MedicalChatMode.WORKFLOW;
            case "auto" -> MedicalChatMode.AUTO;
            default -> MedicalChatMode.AUTO;
        };
    }
}
