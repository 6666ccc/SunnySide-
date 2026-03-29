package cn.lc.sunnyside.Workflow;

import cn.lc.sunnyside.Workflow.common.WorkflowStateKeys;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class SunnySideWorkflowService {

    private final CompiledGraph workflow;

    public SunnySideWorkflowService(
            @Qualifier(SunnySideWorkflowConfig.SUNNY_SIDE_DEMO_WORKFLOW_BEAN) CompiledGraph workflow) {
        this.workflow = workflow;
    }

    public String executeWorkflow(String query) {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put(WorkflowStateKeys.QUERY, query);
        return workflow.invoke(inputs)
                .flatMap(state -> state.value(WorkflowStateKeys.LLM_RESPONSE))
                .map(Object::toString)
                .orElse("");
    }
}
