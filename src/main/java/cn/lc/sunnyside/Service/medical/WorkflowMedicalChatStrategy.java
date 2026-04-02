package cn.lc.sunnyside.Service.medical;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Workflow（StateGraph 两节点：router -> response）策略实现。
 */
@Component
public class WorkflowMedicalChatStrategy implements MedicalChatStrategy {

    private final cn.lc.sunnyside.Workflow.AgentRouterWorkflowService agentRouterWorkflowService;

    public WorkflowMedicalChatStrategy(cn.lc.sunnyside.Workflow.AgentRouterWorkflowService agentRouterWorkflowService) {
        this.agentRouterWorkflowService = agentRouterWorkflowService;
    }

    @Override
    public MedicalChatAttempt tryReply(MedicalChatContext ctx) {
        try {
            String reply = agentRouterWorkflowService.executeWorkflow(ctx.query(), ctx.relativePhone(), ctx.threadId());
            boolean success = StringUtils.hasText(reply);
            return new MedicalChatAttempt(reply != null ? reply : "", success);
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return new MedicalChatAttempt("", false);
        }
    }
}

