package cn.lc.sunnyside.agent;

import cn.lc.sunnyside.Service.RelativeAccessService;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.stereotype.Component;

/**
 * 在模型调用前注入与亲属手机号、默认患者 ID 相关的系统提示（与 MedicalRouterNode 一致）。
 */
@Component
public class MedicalAgentModelContextInterceptor extends ModelInterceptor {

    private final RelativeAccessService relativeAccessService;

    public MedicalAgentModelContextInterceptor(RelativeAccessService relativeAccessService) {
        this.relativeAccessService = relativeAccessService;
    }

    @Override
    public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
        MedicalAgentCallContext ctx = MedicalAgentCallContext.current();
        String fullSystem = ctx == null
                ? (request.getSystemMessage() != null ? request.getSystemMessage().getText() : MedicalAgentPromptBuilder.ROUTER_SYSTEM)
                : MedicalAgentPromptBuilder.build(ctx.relativePhone(), ctx.defaultPatientId(), relativeAccessService);

        SystemMessage systemMessage = new SystemMessage(fullSystem);
        ModelRequest modified = ModelRequest.builder(request).systemMessage(systemMessage).build();
        return handler.call(modified);
    }

    @Override
    public String getName() {
        return "medical_agent_model_context";
    }
}
