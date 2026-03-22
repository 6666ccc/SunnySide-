package cn.lc.sunnyside.Service.ServiceImpl;

import cn.lc.sunnyside.AITool.ElderTool;
import cn.lc.sunnyside.AITool.RelativesTool;
import cn.lc.sunnyside.Auth.FamilyLoginContextHolder;
import cn.lc.sunnyside.POJO.DTO.ChatReply;
import cn.lc.sunnyside.Service.AIService;
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

    @Value("classpath:prompts/system.st")
    private Resource systemPrompt;

    @Value("classpath:prompts/relatives_system.st")
    private Resource relativesSystemPrompt;

    public AIServiceImpl(ChatClient.Builder builder, ChatMemory chatMemory, ElderTool elderTool,
            RelativesTool relativesTool,
            VectorStore vectorStore) {
        this.elderTool = elderTool;
        this.relativesTool = relativesTool;
        this.chatClient = builder
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultAdvisors(
                        QuestionAnswerAdvisor.builder(vectorStore)
                                .searchRequest(SearchRequest.builder().build())
                                .build())
                .build();
    }

    @Override
    public ChatReply.ChatReplyRecord elderChat(String userInput, String conversationId) {
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
     * 构建系统提示模板,以及构建聊天实体
     * 
     * @param systemPrompt 系统提示模板资源
     * @return 构建后的系统提示模板
     */
    @Override
    public ChatReply.ChatReplyRecord relativesChat(String userInput, String conversationId) {
        Prompt prompt = buildSystemPrompt(relativesSystemPrompt);
        String answer = this.chatClient.prompt(prompt)
                .user(userInput)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .tools(elderTool, relativesTool)
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
                .tools(elderTool, relativesTool)
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
        // 构建 multimodal 提示模板
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
     * 构建系统提示模板,以及构建聊天实体(主要将解析后的 家属信息 注入到系统提示模板中)
     * 
     * @param promptResource 系统提示模板资源
     * @return 构建后的系统提示模板
     */
    private Prompt buildSystemPrompt(Resource promptResource) {
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(promptResource);
        String familyContext = FamilyLoginContextHolder.get()
                .map(context -> "当前请求已登录家属信息：familyId=" + context.familyId() + "，familyPhone=" + context.phone()
                        + "。当调用家属相关工具时，优先使用该身份，不要再向用户追问手机号。")
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
                    return mediaFile.getOriginalFilename() != null ? mediaFile.getOriginalFilename() : "file";
                }
            };
        } catch (IOException ex) {
            throw new IllegalStateException("媒体文件读取失败。", ex);
        }
    }
}
