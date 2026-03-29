package cn.lc.sunnyside.Workflow.common;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Optional;

/**
 * 可复用的工作流执行基类：封装 {@link CompiledGraph#invoke}、可选 {@link RunnableConfig}（含 threadId）与指定输出键的读取。
 */
public class BaseWorkflowExecutor {

    /**
     * 执行指定编译图并读取目标输出字段。
     *
     * @param workflow 已编译工作流
     * @param inputs 输入状态
     * @param outputKey 目标输出键
     * @param fallbackError 失败时的兜底文案
     * @return 成功时返回输出文本，否则返回兜底文案
     */
    protected String executeWorkflow(CompiledGraph workflow, Map<String, Object> inputs, String outputKey, String fallbackError) {
        return executeWorkflow(workflow, inputs, outputKey, fallbackError, null);
    }

    /**
     * 执行编译图；若提供 threadId，则写入 {@link RunnableConfig}，便于运行时与检查点/状态历史按会话关联（与官方文档 RunnableConfig 用法一致）。
     */
    protected String executeWorkflow(CompiledGraph workflow, Map<String, Object> inputs, String outputKey,
            String fallbackError, String threadId) {
        try {
            Optional<OverAllState> stateOpt;
            if (StringUtils.hasText(threadId)) {
                stateOpt = workflow.invoke(inputs, RunnableConfig.builder().threadId(threadId.trim()).build());
            } else {
                stateOpt = workflow.invoke(inputs);
            }
            if (stateOpt.isEmpty()) {
                return fallbackError;
            }
            Optional<Object> output = stateOpt.get().value(outputKey);
            if (output.isEmpty()) {
                return fallbackError;
            }
            String text = output.get().toString();
            if (!StringUtils.hasText(text)) {
                return fallbackError;
            }
            return text;
        } catch (Exception e) {
            return fallbackError;
        }
    }
}
