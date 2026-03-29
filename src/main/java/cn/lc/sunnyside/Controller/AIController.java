package cn.lc.sunnyside.Controller;

import cn.lc.sunnyside.Auth.RelativeLoginContext;
import cn.lc.sunnyside.POJO.DTO.ChatReply;
import cn.lc.sunnyside.Service.AIService;
import cn.lc.sunnyside.Workflow.AgentRouterWorkflowService;
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
    private final AgentRouterWorkflowService agentRouterWorkflowService;

    public AIController(AIService aiService, AgentRouterWorkflowService agentRouterWorkflowService) {
        this.aiService = aiService;
        this.agentRouterWorkflowService = agentRouterWorkflowService;
    }

    @GetMapping("/PatientChat")
    public ChatReply.ChatReplyRecord patientChat(
            @RequestParam("userInput") String userInput,
            @RequestParam(value = "UserID", required = false) String legacyUserId) {
        String conversationId = legacyUserId;
        if (conversationId == null || conversationId.isBlank()) {
            return userIdRequiredReply();
        }
        return aiService.patientChat(userInput, conversationId);
    }

    @GetMapping("/RelativesChat")
    public ChatReply.ChatReplyRecord relativesChat(
            @RequestParam("userInput") String userInput,
            @RequestParam(value = "UserID", required = false) String legacyUserId) {
        String conversationId = legacyUserId;
        if (conversationId == null || conversationId.isBlank()) {
            return userIdRequiredReply();
        }
        return aiService.relativesChat(userInput, conversationId);
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(
            @RequestParam("userInput") String userInput,
            @RequestParam(value = "UserID", required = false) String legacyUserId) {
        String conversationId = legacyUserId;
        if (conversationId == null || conversationId.isBlank()) {
            return Flux.just("userId不能为空。");
        }
        return aiService.streamChat(userInput, conversationId);
    }

    @PostMapping(value = "/chat/multimodal", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ChatReply.ChatReplyRecord multimodalChat(
            @RequestParam("userInput") String userInput,
            @RequestParam("media") List<MultipartFile> mediaFiles,
            @RequestParam(value = "UserID", required = false) String legacyUserId) {
        String conversationId = legacyUserId;
        if (conversationId == null || conversationId.isBlank()) {
            return userIdRequiredReply();
        }
        return aiService.multimodalChat(userInput, mediaFiles, conversationId);
    }

    private ChatReply.ChatReplyRecord userIdRequiredReply() {
        return new ChatReply.ChatReplyRecord("userId不能为空。", List.of());
    }

    @GetMapping("/api/workflow/medical-chat")
    public String medicalWorkflowChat(
            @RequestParam(name = "query", defaultValue = "查一下1号患者的最新体征") String query,
            @RequestParam(name = "phone", required = false) String phone) {

        String relativePhone = phone;
        if (relativePhone == null || relativePhone.isBlank()) {
            relativePhone = RelativeLoginContext.get().map(RelativeLoginContext::phone).orElse(null);
        }

        return agentRouterWorkflowService.executeWorkflow(query, relativePhone, null);
    }
}
