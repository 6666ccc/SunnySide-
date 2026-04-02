package cn.lc.sunnyside.Service;

import cn.lc.sunnyside.Service.medical.AgentLoopMedicalChatStrategy;
import cn.lc.sunnyside.Service.medical.MedicalChatAttempt;
import cn.lc.sunnyside.Service.medical.MedicalChatContext;
import cn.lc.sunnyside.Service.medical.MedicalChatMode;
import cn.lc.sunnyside.Service.medical.MedicalChatStrategy;
import cn.lc.sunnyside.Service.medical.WorkflowMedicalChatStrategy;
import cn.lc.sunnyside.AITool.InpatientMedicalTools;
import cn.lc.sunnyside.Auth.RelativeLoginContext;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class MedicalChatOrchestratorService {

    private final MedicalChatStrategy agentLoopStrategy;
    private final MedicalChatStrategy workflowStrategy;

    private final ChatClient chatClient;
    private final InpatientMedicalTools medicalTools;
    private final RelativeAccessService relativeAccessService;
    private final Resource relativesSystemPrompt;

    public MedicalChatOrchestratorService(
            ChatClient.Builder builder,
            ChatMemory chatMemory,
            VectorStore vectorStore,
            InpatientMedicalTools medicalTools,
            RelativeAccessService relativeAccessService,
            @Value("classpath:prompts/relatives_system.st") Resource relativesSystemPrompt,
            AgentLoopMedicalChatStrategy agentLoopStrategy,
            WorkflowMedicalChatStrategy workflowStrategy) {

        this.agentLoopStrategy = agentLoopStrategy;
        this.workflowStrategy = workflowStrategy;
        this.medicalTools = medicalTools;
        this.relativeAccessService = relativeAccessService;
        this.relativesSystemPrompt = relativesSystemPrompt;

        // 复用 AIServiceImpl 的主链路配置：会话记忆 + RAG 检索增强。
        this.chatClient = builder
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultAdvisors(
                        QuestionAnswerAdvisor.builder(vectorStore)
                                .searchRequest(SearchRequest.builder()
                                        .topK(4)
                                        .similarityThreshold(0.72)
                                        .build())
                                .build())
                .build();
    }

    /**
     * 统一医疗对话编排入口。
     *
     * @param query         用户问题
     * @param relativePhone 亲属手机号（来自 login JWT 或 phone 参数）
     * @param threadId      会话隔离 ID（UserID）
     * @param mode          auto/loop/workflow
     * @return 文本回复
     */
    public String execute(String query, String relativePhone, String threadId, MedicalChatMode mode) {
        if (!StringUtils.hasText(query)) {
            return "请输入您的问题。";
        }
        if (!StringUtils.hasText(relativePhone)) {
            return "请先使用亲属账号登录（请求头携带有效 Bearer Token），或在参数中提供 phone。";
        }

        String q = query.trim();
        String phone = relativePhone.trim();
        String tid = StringUtils.hasText(threadId) ? threadId.trim() : null;

        MedicalChatContext ctx = new MedicalChatContext(q, phone, tid);

        MedicalChatMode actualMode = mode != null ? mode : MedicalChatMode.AUTO;
        return switch (actualMode) {
            case LOOP -> executeLoopFixed(ctx);
            case WORKFLOW -> executeWorkflowFixed(ctx);
            case AUTO -> executeAuto(ctx);
        };
    }

    private String executeAuto(MedicalChatContext ctx) {
        // auto：优先 loop；loop 不可用或不可用结果 -> workflow（如启用）；再不行 -> ChatClient 兜底
        MedicalChatAttempt loopAttempt = agentLoopStrategy.tryReply(ctx);
        if (loopAttempt.success()) {
            return safeText(loopAttempt.reply());
        }
        MedicalChatAttempt workflowAttempt = workflowStrategy.tryReply(ctx);
        if (workflowAttempt.success()) {
            return safeText(workflowAttempt.reply());
        }
        return chatClientMedicalFallback(ctx);
    }

    private String executeLoopFixed(MedicalChatContext ctx) {
        MedicalChatAttempt loopAttempt = agentLoopStrategy.tryReply(ctx);
        if (StringUtils.hasText(loopAttempt.reply())) {
            return loopAttempt.reply();
        }
        return chatClientMedicalFallback(ctx);
    }

    private String executeWorkflowFixed(MedicalChatContext ctx) {
        MedicalChatAttempt workflowAttempt = workflowStrategy.tryReply(ctx);
        if (StringUtils.hasText(workflowAttempt.reply())) {
            return workflowAttempt.reply();
        }
        return chatClientMedicalFallback(ctx);
    }

    private String safeText(String text) {
        return text == null ? "" : text;
    }

    private String chatClientMedicalFallback(MedicalChatContext ctx) {
        try {
            Prompt prompt = buildRelativesSystemPrompt(ctx.relativePhone());
            if (StringUtils.hasText(ctx.threadId())) {
                return chatClient.prompt(prompt)
                        .user(ctx.query())
                        .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, ctx.threadId()))
                        .tools(medicalTools)
                        .call()
                        .content();
            }
            return chatClient.prompt(prompt)
                    .user(ctx.query())
                    .tools(medicalTools)
                    .call()
                    .content();
        } catch (Exception e) {
            return "抱歉，当前无法处理您的请求，请稍后再试。";
        }
    }

    private Prompt buildRelativesSystemPrompt(String relativePhone) {
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(relativesSystemPrompt);

        String familyContext = RelativeLoginContext.get()
                .map(context -> "当前请求已登录亲属信息：relativeId=" + context.relativeId()
                        + "，relativePhone=" + context.phone()
                        + "。" + relativeAccessService.buildBoundPatientContext(context.phone())
                        + " 当调用亲属相关工具时，优先使用登录态与默认关联患者，不要再向用户追问手机号。")
                .orElseGet(() -> {
                    String phone = relativePhone != null ? relativePhone.trim() : "";
                    String bound = relativeAccessService.buildBoundPatientContext(phone);
                    return "当前请求未识别到已登录亲属身份。请求参数提供 phone=" + phone
                            + "，关联患者如下：" + bound
                            + " 当调用亲属相关工具时，优先使用请求参数 phone 对应的默认关联患者，不要再向用户追问手机号。";
                });

        return new Prompt(List.of(systemPromptTemplate.createMessage(Map.of(
                "current_time", LocalDateTime.now().toString(),
                "family_context", familyContext
        ))));
    }
}

