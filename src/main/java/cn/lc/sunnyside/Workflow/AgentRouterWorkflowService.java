package cn.lc.sunnyside.Workflow;

import cn.lc.sunnyside.Workflow.common.BaseWorkflowExecutor;
import cn.lc.sunnyside.Workflow.common.MedicalWorkflowKeys;
import cn.lc.sunnyside.Workflow.common.WorkflowStateKeys;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 住院医疗总控工作流门面：注入 medicalWorkflow，组装 query / 会话 ID 后同步执行并返回 FINAL_REPLY。
 */
@Service
public class AgentRouterWorkflowService extends BaseWorkflowExecutor {

    private final CompiledGraph workflow;

    public AgentRouterWorkflowService(@Qualifier("medicalWorkflow") CompiledGraph workflow) {
        this.workflow = workflow;
    }

    public String executeWorkflow(String query, String relativePhone, String conversationId) {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put(MedicalWorkflowKeys.QUERY, query);
        if (StringUtils.hasText(relativePhone)) {
            inputs.put(WorkflowStateKeys.RELATIVE_PHONE, relativePhone.trim());
        }
        if (StringUtils.hasText(conversationId)) {
            inputs.put(WorkflowStateKeys.CONVERSATION_ID, conversationId);
        }
        return executeWorkflow(workflow, inputs, MedicalWorkflowKeys.FINAL_REPLY, "", conversationId);
    }
}
