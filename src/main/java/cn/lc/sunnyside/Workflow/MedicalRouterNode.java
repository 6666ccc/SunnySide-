package cn.lc.sunnyside.Workflow;

import cn.lc.sunnyside.AITool.InpatientMedicalTools;
import cn.lc.sunnyside.Service.RelativeAccessService;
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

    private static final String ROUTER_SYSTEM = """
            你是住院医疗智能助手。当用户询问患者的生命体征（血压、心率、体温、血氧等）、诊疗计划（输液、手术、检查安排）、
            值班医疗团队（主治医生、责任护士）、饮食医嘱（餐食安排、禁忌）等问题时，必须调用提供的工具获取真实数据，不要编造。
            若系统提供了「默认患者ID」，调用住院业务工具时必须优先使用该患者ID，除非用户明确指定其他患者ID或能根据姓名在上下文中唯一确定另一名患者。
            如果用户问题中包含患者ID，也可直接使用；如果提到科室ID，也直接使用。
            可根据需要连续调用多个工具。若问题与住院医疗业务无关，可不调用工具。""";

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
        if (phone == null) {
            return ROUTER_SYSTEM;
        }
        StringBuilder sb = new StringBuilder(ROUTER_SYSTEM);
        sb.append("\n\n【亲属与患者上下文】\n");
        sb.append(relativeAccessService.buildBoundPatientContext(phone));
        state.value(WorkflowStateKeys.PATIENT_ID).ifPresent(pid -> {
            Long id = toLong(pid);
            if (id != null) {
                sb.append("\n当前会话默认患者ID（工具入参 patientId 请使用该值，除非用户明确要求其他患者）：").append(id);
            }
        });
        return sb.toString();
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
