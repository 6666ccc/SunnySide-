package cn.lc.sunnyside.agent;

import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallResponse;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolInterceptor;
import org.springframework.stereotype.Component;

/**
 * 工具异常时返回可读说明，避免单次工具失败直接中断整条 ReAct 链路。
 */
@Component
public class MedicalToolErrorInterceptor extends ToolInterceptor {

    @Override
    public ToolCallResponse interceptToolCall(ToolCallRequest request, ToolCallHandler handler) {
        try {
            return handler.call(request);
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return ToolCallResponse.of(request.getToolCallId(), request.getToolName(), "Tool failed: " + msg);
        }
    }

    @Override
    public String getName() {
        return "medical_tool_error";
    }
}
