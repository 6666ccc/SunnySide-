package cn.lc.sunnyside.Workflow;

import cn.lc.sunnyside.AITool.InpatientMedicalTools;
import cn.lc.sunnyside.Workflow.common.MedicalWorkflowKeys;
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
            如果用户问题中包含患者ID，直接使用；如果提到科室ID，也直接使用。
            可根据需要连续调用多个工具。若问题与住院医疗业务无关，可不调用工具。""";

    private final ChatClient chatClient;
    private final InpatientMedicalTools medicalTools;

    public MedicalRouterNode(ChatClient.Builder chatClientBuilder, InpatientMedicalTools medicalTools) {
        this.chatClient = chatClientBuilder.build();
        this.medicalTools = medicalTools;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        Map<String, Object> result = new HashMap<>();
        String query = state.value(MedicalWorkflowKeys.QUERY).map(Object::toString).orElse("").trim();

        Map<String, Object> toolContext = new HashMap<>();
        if (!query.isEmpty()) {
            String llmOutput = chatClient.prompt()
                    .system(ROUTER_SYSTEM)
                    .user(query)
                    .tools(medicalTools)
                    .call()
                    .content();
            toolContext.put("llm_tool_output", llmOutput != null ? llmOutput : "");
        }

        result.put(MedicalWorkflowKeys.TOOL_CONTEXT, toolContext);
        return result;
    }
}
