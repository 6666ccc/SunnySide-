package cn.lc.sunnyside.Service.medical;

import cn.lc.sunnyside.Service.AgentLoopMedicalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Agent Loop（ReactAgent 显式 ReAct 循环）策略实现。
 */
@Component
public class AgentLoopMedicalChatStrategy implements MedicalChatStrategy {

    private final AgentLoopMedicalService agentLoopMedicalService;

    public AgentLoopMedicalChatStrategy(@Autowired(required = false) AgentLoopMedicalService agentLoopMedicalService) {
        this.agentLoopMedicalService = agentLoopMedicalService;
    }

    @Override
    public MedicalChatAttempt tryReply(MedicalChatContext ctx) {
        if (agentLoopMedicalService == null) {
            return new MedicalChatAttempt("Agent Loop 未启用，请在配置中设置 app.ai.agent-loop.enabled=true。", false);
        }
        if (!StringUtils.hasText(ctx.threadId())) {
            return new MedicalChatAttempt("userId不能为空。", false);
        }

        try {
            String reply = agentLoopMedicalService.execute(ctx.query(), ctx.relativePhone(), ctx.threadId());
            boolean success = StringUtils.hasText(reply) && !reply.startsWith("Agent 执行异常：");
            return new MedicalChatAttempt(reply != null ? reply : "", success);
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return new MedicalChatAttempt("Agent 执行异常：" + msg, false);
        }
    }
}

