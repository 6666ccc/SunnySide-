package cn.lc.sunnyside.Service.ServiceImpl;

import cn.lc.sunnyside.AITool.InpatientMedicalTools;
import cn.lc.sunnyside.Auth.RelativeLoginContext;
import cn.lc.sunnyside.POJO.DTO.ChatReply;
import cn.lc.sunnyside.Service.AIService;
import cn.lc.sunnyside.Service.MedicalChatOrchestratorService;
import cn.lc.sunnyside.Service.RelativeAccessService;
import cn.lc.sunnyside.Service.medical.MedicalChatMode;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * @author lc
 */
@Service
public class AIServiceImpl implements AIService {

    private final ChatClient chatClient;
    private final InpatientMedicalTools medicalTools;
    private final RelativeAccessService relativeAccessService;
    private final MedicalChatOrchestratorService medicalChatOrchestratorService;

    @Value("classpath:prompts/system.st")
    private Resource systemPrompt;

    @Value("classpath:prompts/relatives_system.st")
    private Resource relativesSystemPrompt;

    public AIServiceImpl(ChatClient.Builder builder,
                         ChatMemory chatMemory,
                         InpatientMedicalTools medicalTools,
                         RelativeAccessService relativeAccessService,
                         MedicalChatOrchestratorService medicalChatOrchestratorService,
                         VectorStore vectorStore) {
        this.medicalTools = medicalTools;
        this.relativeAccessService = relativeAccessService;
        this.medicalChatOrchestratorService = medicalChatOrchestratorService;

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

    @Override
    public ChatReply.ChatReplyRecord patientChat(String userInput, String conversationId) {
        Prompt prompt = buildSystemPrompt(systemPrompt);
        String answer = this.chatClient.prompt(prompt)
                .user(userInput)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .tools(medicalTools)
                .call()
                .content();
        return new ChatReply.ChatReplyRecord(answer, List.of());
    }

    @Override
    public ChatReply.ChatReplyRecord relativesChat(String userInput, String conversationId) {
        String workflowReply = tryWorkflowReply(userInput, conversationId);
        if (StringUtils.hasText(workflowReply)) {
            return new ChatReply.ChatReplyRecord(workflowReply, List.of());
        }
        Prompt prompt = buildSystemPrompt(relativesSystemPrompt);
        String answer = this.chatClient.prompt(prompt)
                .user(userInput)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .tools(medicalTools)
                .call()
                .content();
        return new ChatReply.ChatReplyRecord(answer, List.of());
    }

    @Override
    public Flux<String> streamChat(String userInput, String conversationId) {
        Prompt prompt = buildSystemPrompt(systemPrompt);
        return this.chatClient.prompt(prompt)
                .user(userInput)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .tools(medicalTools)
                .stream()
                .content();
    }

    @Override
    public ChatReply.ChatReplyRecord multimodalChat(String userInput, List<MultipartFile> mediaFiles,
            String conversationId) {
        if (mediaFiles == null || mediaFiles.isEmpty() || mediaFiles.stream().allMatch(MultipartFile::isEmpty)) {
            return new ChatReply.ChatReplyRecord("请上传媒体文件。", List.of());
        }

        for (MultipartFile mediaFile : mediaFiles) {
            if (mediaFile == null || mediaFile.isEmpty()) {
                continue;
            }
            String contentType = normalizeContentType(mediaFile.getContentType());
            if (contentType == null) {
                return new ChatReply.ChatReplyRecord("无法识别媒体类型，请上传带有Content-Type的文件。", List.of());
            }
            if (!isSupportedMultimodalType(contentType)) {
                return new ChatReply.ChatReplyRecord("仅支持 image/*、audio/*、video/* 类型的媒体输入。", List.of());
            }
        }

        Prompt prompt = buildSystemPrompt(systemPrompt);
        String answer = this.chatClient.prompt(prompt)
                .options(DashScopeChatOptions.builder().multiModel(true).build())
                .user(u -> {
                    u.text(userInput);
                    for (MultipartFile mediaFile : mediaFiles) {
                        if (mediaFile == null || mediaFile.isEmpty()) {
                            continue;
                        }
                        String contentType = normalizeContentType(mediaFile.getContentType());
                        MimeType mimeType = null;
                        if (contentType != null) {
                            mimeType = MimeTypeUtils.parseMimeType(contentType);
                        }
                        if (mimeType != null) {
                            u.media(mimeType, toNamedResource(mediaFile));
                        }
                    }
                })
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .tools(medicalTools)
                .call()
                .content();
        return new ChatReply.ChatReplyRecord(answer, List.of());
    }

    private Prompt buildSystemPrompt(Resource promptResource) {
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(promptResource);

        String relativeContext = RelativeLoginContext.get()
                .map(context -> "当前请求已登录亲属信息：relativeId=" + context.relativeId() + "，relativePhone=" + context.phone()
                        + "。" + relativeAccessService.buildBoundPatientContext(context.phone())
                        + " 当调用亲属相关工具时，优先使用登录态与默认关联患者，不要再向用户追问手机号。")
                .orElse("当前请求未识别到已登录亲属身份。");

        Message systemMessage = systemPromptTemplate
                .createMessage(Map.of(
                        "current_time", LocalDateTime.now().toString(),
                        "family_context", relativeContext));
        return new Prompt(List.of(systemMessage));
    }

    private boolean isSupportedMultimodalType(String contentType) {
        return contentType.startsWith("image/")
                || contentType.startsWith("audio/")
                || contentType.startsWith("video/");
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return null;
        }
        return contentType.toLowerCase(Locale.ROOT);
    }

    private Resource toNamedResource(MultipartFile mediaFile) {
        try {
            return new ByteArrayResource(mediaFile.getBytes()) {
                @Override
                public String getFilename() {
                    return mediaFile.getOriginalFilename() != null ? mediaFile.getOriginalFilename() : "file";
                }
            };
        } catch (IOException ex) {
            throw new IllegalStateException("媒体文件读取失败。", ex);
        }
    }

    private String tryWorkflowReply(String userInput, String conversationId) {
        if (!StringUtils.hasText(userInput)) {
            return null;
        }
        String relativePhone = RelativeLoginContext.get()
                .map(RelativeLoginContext::phone)
                .orElse(null);
        return medicalChatOrchestratorService.execute(userInput, relativePhone, conversationId, MedicalChatMode.AUTO);
    }
}
