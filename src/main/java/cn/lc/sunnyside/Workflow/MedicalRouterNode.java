package cn.lc.sunnyside.Workflow;

import cn.lc.sunnyside.AITool.InpatientMedicalTools;
import cn.lc.sunnyside.Service.RelativeAccessService;
import cn.lc.sunnyside.agent.MedicalAgentPromptBuilder;
import cn.lc.sunnyside.Workflow.common.MedicalWorkflowKeys;
import cn.lc.sunnyside.Workflow.common.WorkflowStateKeys;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 住院医疗工作流路由节点：使用 LLM Function Calling 根据用户查询自动调用 InpatientMedicalTools，
 * 将工具返回的数据存入 TOOL_CONTEXT。
 */
@Component
public class MedicalRouterNode implements NodeAction {

    private final ChatClient chatClient;
    private final InpatientMedicalTools medicalTools;
    private final RelativeAccessService relativeAccessService;

    public MedicalRouterNode(ChatClient.Builder chatClientBuilder, InpatientMedicalTools medicalTools,
            RelativeAccessService relativeAccessService) {
        this.chatClient = chatClientBuilder.build();
        this.medicalTools = medicalTools;
        this.relativeAccessService = relativeAccessService;
    }

    /**
     * 应用路由节点
     * @param state 状态
     * @return 结果
     * @throws Exception 异常
     */
    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        Map<String, Object> result = new HashMap<>();
        String query = state.value(MedicalWorkflowKeys.QUERY).map(Object::toString).orElse("").trim();

        Map<String, Object> toolContext = new HashMap<>();
        if (!query.isEmpty()) {
            String systemPrompt = buildRouterSystemPrompt(state);
            String llmOutput = chatClient.prompt()
                    .system(systemPrompt)
                    .user(query)
                    .tools(medicalTools)
                    .call()
                    .content();
            toolContext.put("llm_tool_output", llmOutput != null ? llmOutput : "");
        }

        result.put(MedicalWorkflowKeys.TOOL_CONTEXT, toolContext);
        return result;
    }

    /**
     * 构建路由系统提示词
     * @param state 状态
     * @return 提示词
     */
    private String buildRouterSystemPrompt(OverAllState state) {
        String phone = state.value(WorkflowStateKeys.RELATIVE_PHONE)
                .map(Object::toString)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .orElse(null);
        Long defaultPatientId = state.value(WorkflowStateKeys.PATIENT_ID)
                .map(MedicalRouterNode::toLong)
                .orElse(null);
        return MedicalAgentPromptBuilder.build(phone, defaultPatientId, relativeAccessService);
    }

    /**
     * 将对象转换为Long
     * @param value 对象
     * @return Long
     */
    private static Long toLong(Object value) {
        if (value instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(value.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
