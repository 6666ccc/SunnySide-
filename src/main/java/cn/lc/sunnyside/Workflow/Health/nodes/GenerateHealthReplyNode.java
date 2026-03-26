package cn.lc.sunnyside.Workflow.Health.nodes;

import cn.lc.sunnyside.Workflow.common.WorkflowStateKeys;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Component
public class GenerateHealthReplyNode implements NodeAction {
    private final ChatClient chatClient;

    /**
     * 构造健康回复生成节点。
     *
     * @param builder ChatClient 构造器
     */
    public GenerateHealthReplyNode(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    /**
     * 将健康业务结果组织成自然语言回复。
     * 当存在错误信息时直接透传错误文案。
     *
     * @param state 当前工作流状态
     * @return 写入最终回复字段的状态增量
     */
    @Override
    public Map<String, Object> apply(OverAllState state) {
        Map<String, Object> result = new HashMap<>();
        String query = state.value(WorkflowStateKeys.QUERY).map(Object::toString).orElse("");
        String errorMessage = state.value(WorkflowStateKeys.ERROR_MESSAGE).map(Object::toString).orElse(null);
        if (StringUtils.hasText(errorMessage)) {
            result.put(WorkflowStateKeys.FINAL_REPLY, errorMessage);
            return result;
        }

        String intent = state.value(WorkflowStateKeys.INTENT).map(Object::toString).orElse("GENERAL");
        if (!"HEALTH_QUERY".equalsIgnoreCase(intent)) {
            result.put(WorkflowStateKeys.FINAL_REPLY, "");
            return result;
        }
        String healthData = state.value(WorkflowStateKeys.BIZ_RESULT).map(Object::toString).orElse("");
        String prompt = String.format(
                "你是养老院健康助手。请严格按“结论 + 关键依据 + 下一步”回复。\n" +
                        "家属提问：%s\n" +
                        "系统健康数据：%s\n" +
                        "要求：专业、温和、简洁，不暴露系统实现细节。",
                query, healthData);
        String answer;
        try {
            answer = chatClient.prompt().user(prompt).call().content();
        } catch (Exception e) {
            answer = healthData;
        }
        result.put(WorkflowStateKeys.FINAL_REPLY, answer);
        return result;
    }
}
