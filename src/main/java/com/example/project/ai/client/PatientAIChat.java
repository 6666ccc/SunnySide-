package com.example.project.ai.client;

import java.time.LocalDate;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.example.project.ai.prompt.PatientChatSystemPrompt;
import com.example.project.ai.tools.PatientAITool;
import com.example.project.service.PatientAuthService;

import reactor.core.publisher.Flux;

@Component
public class PatientAIChat {

    @Value("classpath:prompt/patient-system.st")
    private Resource templateResource;

    @Autowired
    private PatientAITool patientAITool;

    @Autowired
    private PatientAuthService patientAuthService;

    private final ChatClient chatClient;

    public PatientAIChat(ChatClient.Builder chatClient, Advisor retrievalAugmentationAdvisor,
            MessageChatMemoryAdvisor messageChatMemoryAdvisor) {
        this.chatClient = chatClient
                .defaultAdvisors(retrievalAugmentationAdvisor, messageChatMemoryAdvisor)
                .build();
    }

    /**
     * @param jwtSubject 与拦截器写入的 {@code userId} 一致（JWT subject，患者端为用户名）
     */
    public Flux<String> stream(String timeId, String message, String jwtSubject) {
        String loginUsername = StringUtils.hasText(jwtSubject) ? jwtSubject.trim() : "（未登录）";
        Long patientId = patientAuthService.resolvePatientUserId(loginUsername);
        String patientIdLine = patientId != null ? String.valueOf(patientId) : "（未能解析，请确认患者端已登录）";

        PromptTemplate template = new PromptTemplate(templateResource);
        String systemPrompt = template.render(Map.of(
                "baseRules", PatientChatSystemPrompt.baseRules(),
                "patientId", patientIdLine,
                "loginUsername", loginUsername,
                "serverDate", LocalDate.now().toString()));

        return chatClient.prompt()
                .system(systemPrompt)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, timeId))
                .tools(patientAITool)
                .user(message)
                .stream()
                .content();
    }
}
