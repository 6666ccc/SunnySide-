package cn.lc.sunnyside.Workflow;

import cn.lc.sunnyside.Workflow.common.WorkflowStateKeys;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 演示用工作流节点：根据 {@link WorkflowStateKeys#PROCESSED_QUERY} 调用 ChatClient，将回复写入 {@link WorkflowStateKeys#LLM_RESPONSE}。
 */
@Component
public class SunnySideLlmProcessNode implements NodeAction {

    private final ChatClient chatClient;

    public SunnySideLlmProcessNode(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String query = state.value(WorkflowStateKeys.PROCESSED_QUERY).map(Object::toString).orElse("");
        String answer = query.isEmpty() ? "请输入您的问题。" : chatClient.prompt().user(query).call().content();
        Map<String, Object> result = new HashMap<>();
        result.put(WorkflowStateKeys.LLM_RESPONSE, answer);
        return result;
    }
}
