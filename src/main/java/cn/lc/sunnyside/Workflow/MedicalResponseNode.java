package cn.lc.sunnyside.Workflow;

import cn.lc.sunnyside.Workflow.common.MedicalWorkflowKeys;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 住院医疗工作流响应节点：基于 TOOL_CONTEXT 和用户原始查询，生成专业、温和、简洁的中文医疗回复。
 */
@Component
public class MedicalResponseNode implements NodeAction {

    private static final String RESPONSE_SYSTEM = """
            你是住院患者关怀助手。请根据「用户提问」与「系统工具返回的医疗数据」生成一段专业、温和、简洁的中文回复。
            结构建议：先给出关键结论，再列出事实数据（仅使用工具数据，勿臆测），最后可给一句实用的健康建议。
            使用专业医疗术语但确保患者家属能理解。若无工具数据，则基于问题做一般性关怀回应，不要假装有院内数据。""";

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MedicalResponseNode(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> apply(OverAllState state) throws Exception {
        Map<String, Object> result = new HashMap<>();
        // 获取用户输入的问题/消息
        String query = state.value(MedicalWorkflowKeys.QUERY)
                .map(Object::toString)
                .filter(s -> !s.isEmpty())
                .orElse("");
        // 获取工具上下文
        Map<String, Object> toolCtx = state.value(MedicalWorkflowKeys.TOOL_CONTEXT)
                .filter(Map.class::isInstance)
                .map(v -> (Map<String, Object>) v)
                .orElse(Map.of());

        String toolBlock = formatToolContext(toolCtx);
        String userPayload = "用户提问：\n" + (query.isEmpty() ? "（空）" : query) + "\n\n系统工具数据（JSON）：\n" + toolBlock;

        String answer;
        try {
            answer = chatClient.prompt()
                    .system(RESPONSE_SYSTEM)
                    .user(userPayload)
                    .call()
                    .content();
        } catch (Exception e) {
            answer = fallbackReply(query, toolCtx);
        }
        if (!StringUtils.hasText(answer)) {
            answer = fallbackReply(query, toolCtx);
        }
        result.put(MedicalWorkflowKeys.FINAL_REPLY, answer);
        return result;
    }

    private String formatToolContext(Map<String, Object> toolCtx) {
        if (toolCtx == null || toolCtx.isEmpty()) {
            return "{}";
        }
        try {
            //将工具上下文转换为JSON字符串
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(toolCtx);
        } catch (JsonProcessingException e) {
            //如果转换失败则返回工具上下文的字符串表示
            return toolCtx.toString();
        }
    }

    private String fallbackReply(String query, Map<String, Object> toolCtx) {
        if (toolCtx != null && !toolCtx.isEmpty()) {
            return String.join("\n", toolCtx.values().stream().map(Object::toString).toList());
        }
        if (StringUtils.hasText(query)) {
            return "您好，我们已收到您的问题。如需查询生命体征、诊疗计划、值班医护或饮食安排，请尽量说具体一些，我会为您核实。";
        }
        return "请输入您的问题，我们会尽力协助。";
    }
}
