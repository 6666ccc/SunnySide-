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
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;


@RestController
public class AIController {
    private final ChatClient chatClient;
    private final DailyTool dailyTool;


    @Value("classpath:prompts/system.st")
    private Resource systemPrompt;



    public AIController(ChatClient.Builder builder, ChatMemory chatMemory, DailyTool dailyTool, VectorStore vectorStore) {
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

    private Prompt buildSystemPrompt() {
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(systemPrompt);
        Message systemMessage = systemPromptTemplate.createMessage(Map.of("current_time", LocalDateTime.now().toString()));
        return new Prompt(List.of(systemMessage));
    }
    
}
