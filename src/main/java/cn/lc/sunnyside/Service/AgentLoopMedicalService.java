package cn.lc.sunnyside.Service;

import cn.lc.sunnyside.agent.MedicalAgentCallContext;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 通过 ReactAgent 执行住院医疗多轮工具循环；会话隔离依赖 RunnableConfig.threadId（MemorySaver）。
 */
@Service
@ConditionalOnProperty(prefix = "app.ai.agent-loop", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AgentLoopMedicalService {

    private final ReactAgent medicalReactAgent;
    private final RelativeAccessService relativeAccessService;

    public AgentLoopMedicalService(ReactAgent medicalReactAgent, RelativeAccessService relativeAccessService) {
        this.medicalReactAgent = medicalReactAgent;
        this.relativeAccessService = relativeAccessService;
    }

    /**
     * @param query         用户问题
     * @param relativePhone 亲属手机号（已校验非空）
     * @param threadId      会话标识，写入检查点 threadId（建议与现有 UserID 一致）
     */
    public String execute(String query, String relativePhone, String threadId) {
        if (!StringUtils.hasText(query)) {
            return "请输入您的问题。";
        }
        
        String phone = relativePhone.trim();
        //获取默认患者ID
        Long defaultPatientId = relativeAccessService.resolveDefaultPatientId(phone);
        //设置亲属/患者上下文
        MedicalAgentCallContext.set(new MedicalAgentCallContext(phone, defaultPatientId));
        //创建RunnableConfig
        try {
            //创建RunnableConfig目的是设置会话标识，写入检查点 threadId（建议与现有 UserID 一致）
            RunnableConfig config = StringUtils.hasText(threadId)
                    ? RunnableConfig.builder().threadId(threadId.trim()).build()
                    : RunnableConfig.builder().build();
            //调用ReactAgent尝试获取回复(这是进入Agent Loop的关键点)
            AssistantMessage response = medicalReactAgent.call(query.trim(), config);
            //返回回复
            return response.getText() != null ? response.getText() : "";
        } catch (GraphRunnerException e) {
            //返回异常信息
            return "Agent 执行异常：" + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        } finally {
            //清除亲属/患者上下文
            MedicalAgentCallContext.clear();
        }
    }
}
