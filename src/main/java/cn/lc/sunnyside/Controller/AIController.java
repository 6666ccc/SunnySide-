package cn.lc.sunnyside.Controller;

import cn.lc.sunnyside.AITool.DailyTool;
import cn.lc.sunnyside.POJO.DTO.ChatReply;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.http.MediaType;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import java.io.IOException;

@RestController
public class AIController {
    private final ChatClient chatClient;
    private final DailyTool dailyTool;

    @Value("classpath:prompts/system.st")
    private Resource systemPrompt;

    public AIController(ChatClient.Builder builder, ChatMemory chatMemory, DailyTool dailyTool,
            VectorStore vectorStore) {
        this.dailyTool = dailyTool;
        this.chatClient = builder
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultAdvisors(
                        QuestionAnswerAdvisor.builder(vectorStore)
                                .searchRequest(SearchRequest.builder().build())
                                .build())
                .build();
    }

    @GetMapping("/chat")
    public ChatReply.ChatReplyRecord chat(
            @RequestParam("userInput") String userInput,
            @RequestParam(value = "userId", required = false) String userId,
            @RequestParam(value = "UserID", required = false) String legacyUserId) {
        String conversationId = (userId == null || userId.isBlank()) ? legacyUserId : userId;
        if (conversationId == null || conversationId.isBlank()) {
            return new ChatReply.ChatReplyRecord("userId不能为空。", List.of());
        }
        Prompt prompt = buildSystemPrompt();
        String answer = this.chatClient.prompt(prompt)
                .user(userInput)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .tools(dailyTool)
                .call()
                .content();
        return new ChatReply.ChatReplyRecord(answer, List.of());
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(
            @RequestParam("userInput") String userInput,
            @RequestParam(value = "userId", required = false) String userId,
            @RequestParam(value = "UserID", required = false) String legacyUserId) {
        String conversationId = (userId == null || userId.isBlank()) ? legacyUserId : userId;
        if (conversationId == null || conversationId.isBlank()) {
            return Flux.just("userId不能为空。");
        }
        Prompt prompt = buildSystemPrompt();
        return this.chatClient.prompt(prompt)
                .user(userInput)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .tools(dailyTool)
                .stream()
                .content();
    }

    /**
     * 多模态聊天接口
     * 
     * @param userInput    用户输入文本
     * @param mediaFiles   上传的媒体文件列表
     * @param userId       用户ID（可选）
     * @param legacyUserId 旧版用户ID（可选）
     * @return 聊天回复记录
     */

    @PostMapping(value = "/chat/multimodal", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ChatReply.ChatReplyRecord multimodalChat(
            @RequestParam("userInput") String userInput,
            @RequestParam("media") List<MultipartFile> mediaFiles,
            @RequestParam(value = "userId", required = false) String userId,
            @RequestParam(value = "UserID", required = false) String legacyUserId) {
        String conversationId = (userId == null || userId.isBlank()) ? legacyUserId : userId;
        if (conversationId == null || conversationId.isBlank()) {
            return new ChatReply.ChatReplyRecord("userId不能为空。", List.of());
        }
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
        Prompt prompt = buildSystemPrompt();
        String answer = this.chatClient.prompt(prompt)
                .options(DashScopeChatOptions.builder().multiModel(true).build())//开启多模态输入支
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
                .tools(dailyTool)
                .call()
                .content();
        return new ChatReply.ChatReplyRecord(answer, List.of());
    }

    private Prompt buildSystemPrompt() {
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(systemPrompt);
        Message systemMessage = systemPromptTemplate
                .createMessage(Map.of("current_time", LocalDateTime.now().toString()));
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

}
