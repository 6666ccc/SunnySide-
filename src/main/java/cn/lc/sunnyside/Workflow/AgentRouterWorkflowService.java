package cn.lc.sunnyside.Workflow;

import cn.lc.sunnyside.Service.RelativeAccessService;
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
    private final RelativeAccessService relativeAccessService;

    public AgentRouterWorkflowService(@Qualifier("medicalWorkflow") CompiledGraph workflow,
            RelativeAccessService relativeAccessService) {
        this.workflow = workflow;
        this.relativeAccessService = relativeAccessService;
    }

    /**
     * 执行工作流前的准备工作(将前置信息比如:家属手机号,患者ID,家属/患者提出的问题,会话ID等写入HashMap便于传递给工作流)
     * @param query 用户输入的问题/消息
     * @param relativePhone 用户手机号
     * @param conversationId 会话ID
     * @return 工作流执行结果
     */
    public String executeWorkflow(String query, String relativePhone, String conversationId) {

        Map<String, Object> inputs = new HashMap<>();
        inputs.put(MedicalWorkflowKeys.QUERY, query);

        if (StringUtils.hasText(relativePhone)) {
            String phone = relativePhone.trim();
            inputs.put(WorkflowStateKeys.RELATIVE_PHONE, phone);
            // 尝试获取用户绑定的患者ID(有了ID就可以获得患者信息)
            Long defaultPatientId = relativeAccessService.resolveDefaultPatientId(phone);
            if (defaultPatientId != null) {
                inputs.put(WorkflowStateKeys.PATIENT_ID, defaultPatientId);
            }
        }
        if (StringUtils.hasText(conversationId)) {
            inputs.put(WorkflowStateKeys.CONVERSATION_ID, conversationId);
        }
        // 开始执行工作流
        return executeWorkflow(workflow, inputs, MedicalWorkflowKeys.FINAL_REPLY, "", conversationId);
    }
}
