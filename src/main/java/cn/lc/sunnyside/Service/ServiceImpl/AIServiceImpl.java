package cn.lc.sunnyside.Service.ServiceImpl;

import cn.lc.sunnyside.AITool.ElderTool;
import cn.lc.sunnyside.AITool.RelativesTool;
import cn.lc.sunnyside.Auth.FamilyLoginContext;
import cn.lc.sunnyside.POJO.DTO.ChatReply;
import cn.lc.sunnyside.Service.AIService;
import cn.lc.sunnyside.Service.FamilyAccessService;
import cn.lc.sunnyside.Workflow.AgentRouterWorkflowService;
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

@Service
public class AIServiceImpl implements AIService {

    private final ChatClient chatClient;
    private final ElderTool elderTool;
    private final RelativesTool relativesTool;
    private final FamilyAccessService familyAccessService;
    private final AgentRouterWorkflowService agentRouterWorkflowService;

    @Value("${app.ai.workflow.enabled:false}")
    private boolean workflowEnabled;

    @Value("classpath:prompts/system.st")
    private Resource systemPrompt;

    @Value("classpath:prompts/relatives_system.st")
    private Resource relativesSystemPrompt;

    /**
     * 构造 AI 服务实现并初始化统一 ChatClient。
     * 默认挂载会话记忆与向量检索增强顾问，使所有对话接口复用同一套基础能力。
     *
     * @param builder                    ChatClient 构造器
     * @param chatMemory                 会话记忆组件
     * @param elderTool                  老人端工具集合
     * @param relativesTool              家属端工具集合
     * @param familyAccessService        家属-老人绑定关系服务
     * @param agentRouterWorkflowService 总控路由工作流服务
     * @param vectorStore                向量库实例
     */
    public AIServiceImpl(ChatClient.Builder builder, ChatMemory chatMemory, ElderTool elderTool,
            RelativesTool relativesTool,
            FamilyAccessService familyAccessService,
            AgentRouterWorkflowService agentRouterWorkflowService,
            VectorStore vectorStore) {
        this.elderTool = elderTool;
        this.relativesTool = relativesTool;
        this.familyAccessService = familyAccessService;
        this.agentRouterWorkflowService = agentRouterWorkflowService;

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
     * 老人端与 AI 的文字聊天接口
     *
     * @param userInput      用户输入的文本信息
     * @param conversationId 对话的唯一标识，用于保持上下文记忆
     * @return 包含 AI 回复记录及可能工具调用记录的聊天结果
     */
    @Override
    public ChatReply.ChatReplyRecord elderChat(String userInput, String conversationId) {
        String workflowReply = tryWorkflowReply(userInput, conversationId);
        if (StringUtils.hasText(workflowReply)) {
            return new ChatReply.ChatReplyRecord(workflowReply, List.of());
        }
        Prompt prompt = buildSystemPrompt(systemPrompt);
        String answer = this.chatClient.prompt(prompt)
                .user(userInput)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .tools(elderTool, relativesTool)
                .call()
                .content();
        return new ChatReply.ChatReplyRecord(answer, List.of());
    }

    /**
     * 家属端与 AI 的文字聊天接口
     * 
     * @param userInput      家属输入的文本信息
     * @param conversationId 对话的唯一标识，用于保持上下文记忆
     * @return 包含 AI 回复记录的聊天结果
     */
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
                .tools(elderTool, relativesTool)
                .call()
                .content();
        return new ChatReply.ChatReplyRecord(answer, List.of());
    }

    /**
     * 流式输出的聊天接口，用于实现打字机效果
     *
     * @param userInput      用户输入的文本信息
     * @param conversationId 对话的唯一标识，用于保持上下文记忆
     * @return 包含回复内容的 Flux 流
     */
    @Override
    public Flux<String> streamChat(String userInput, String conversationId) {
        Prompt prompt = buildSystemPrompt(systemPrompt);
        return this.chatClient.prompt(prompt)
                .user(userInput)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .tools(elderTool, relativesTool)
                .stream()
                .content();
    }

    /**
     * 支持多模态（文本 + 多媒体文件）的聊天接口
     *
     * @param userInput      用户输入的文本信息
     * @param mediaFiles     用户上传的媒体文件列表（支持图片、音频、视频）
     * @param conversationId 对话的唯一标识，用于保持上下文记忆
     * @return 包含 AI 多模态回复记录的聊天结果
     */
    @Override
    public ChatReply.ChatReplyRecord multimodalChat(String userInput, List<MultipartFile> mediaFiles,
            String conversationId) {
        // 校验多媒体文件列表是否为空或全部为空文件
        if (mediaFiles == null || mediaFiles.isEmpty() || mediaFiles.stream().allMatch(MultipartFile::isEmpty)) {
            return new ChatReply.ChatReplyRecord("请上传媒体文件。", List.of());
        }

        // 校验文件类型是否符合多模态处理支持的要求
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

        // 构建系统提示词并执行多模态聊天调用
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
                        MimeType mimeType = MimeTypeUtils.parseMimeType(contentType);
                        u.media(mimeType, toNamedResource(mediaFile));
                    }
                })
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .tools(elderTool, relativesTool)
                .call()
                .content();
        return new ChatReply.ChatReplyRecord(answer, List.of());
    }

    /**
     * 构建系统提示模板，主要将解析后的家属登录态信息和当前时间注入到系统提示模板中，作为 AI 的全局上下文
     * 
     * @param promptResource 系统提示模板对应的文件资源
     * @return 构建并渲染参数后的 Prompt 对象
     */
    private Prompt buildSystemPrompt(Resource promptResource) {
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(promptResource);

        // 从上下文中获取家属登录信息并格式化，用于指导 AI 如何使用家属相关的工具
        String familyContext = FamilyLoginContext.get()
                .map(context -> "当前请求已登录家属信息：familyId=" + context.familyId() + "，familyPhone=" + context.phone()
                        + "。" + familyAccessService.buildBoundElderContext(context.phone())
                        + " 当调用家属相关工具时，优先使用登录态与默认绑定老人，不要再向用户追问手机号。")
                .orElse("当前请求未识别到已登录家属身份。");

        Message systemMessage = systemPromptTemplate
                .createMessage(Map.of(
                        "current_time", LocalDateTime.now().toString(),
                        "family_context", familyContext));
        return new Prompt(List.of(systemMessage));
    }

    /**
     * 检查媒体类型是否为 multimodal 支持的类型
     * 
     * @param contentType 媒体类型
     * @return 如果是 multimodal 支持的类型则返回 true，否则返回 false
     */
    private boolean isSupportedMultimodalType(String contentType) {
        return contentType.startsWith("image/")
                || contentType.startsWith("audio/")
                || contentType.startsWith("video/");
    }

    /**
     * 标准化媒体文件的 Content-Type 字符串
     * 
     * @param contentType 原始 Content-Type 字符串
     * @return 标准化后的 Content-Type 字符串（小写，移除首尾空格），如果为空则返回 null
     */
    private String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return null;
        }
        return contentType.toLowerCase(Locale.ROOT);
    }

    /**
     * 将 MultipartFile 转换为 NamedResource 类型，用于 Spring AI 客户端调用
     * 
     * @param mediaFile multipart 文件
     * @return 转换后的 NamedResource 对象
     */
    private Resource toNamedResource(MultipartFile mediaFile) {
        try {
            return new ByteArrayResource(mediaFile.getBytes()) {
                @Override
                public String getFilename() {
                    if (mediaFile.getOriginalFilename() != null) {
                        return mediaFile.getOriginalFilename();
                    }
                    return "file";
                }
            };
        } catch (IOException ex) {
            throw new IllegalStateException("媒体文件读取失败。", ex);
        }
    }

    /**
     * 尝试使用业务工作流直接生成回复。
     * 仅在工作流开关开启、输入有效且工作流返回非空内容时才生效。
     *
     * @param userInput      用户输入
     * @param conversationId 会话ID
     * @return 工作流回复；不命中时返回 null
     */
    private String tryWorkflowReply(String userInput, String conversationId) {
        if (!workflowEnabled || !StringUtils.hasText(userInput)) {
            return null;
        }
        String familyPhone = FamilyLoginContext.get()
                .map(FamilyLoginContext::phone)
                .orElse(null);
        String reply = agentRouterWorkflowService.executeWorkflow(userInput, familyPhone, conversationId);
        if (!StringUtils.hasText(reply)) {
            return null;
        }
        return reply;
    }
}
