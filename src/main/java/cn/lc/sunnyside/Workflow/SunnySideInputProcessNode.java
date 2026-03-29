package cn.lc.sunnyside.Workflow;

import cn.lc.sunnyside.Workflow.common.WorkflowStateKeys;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 演示用工作流节点：读取 {@link WorkflowStateKeys#QUERY}，去空白后写入 {@link WorkflowStateKeys#PROCESSED_QUERY}。
 */
@Component
public class SunnySideInputProcessNode implements NodeAction {

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String input = state.value(WorkflowStateKeys.QUERY).map(Object::toString).orElse("");
        Map<String, Object> result = new HashMap<>();
        result.put(WorkflowStateKeys.PROCESSED_QUERY, input.trim());
        return result;
    }
}
