package cn.lc.sunnyside.Workflow.common;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Optional;


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
     * 执行编译图；若提供 threadId，则写入 RunnableConfig，便于运行时与检查点/状态历史按会话关联（与官方文档 RunnableConfig 用法一致）。
     * @param workflow 已编译工作流
     * @param inputs 输入状态
     * @param outputKey 目标输出键
     * @param fallbackError 失败时的兜底文案
     * @param threadId 会话ID
     * @return 成功时返回输出文本，否则返回兜底文案
     */
    protected String executeWorkflow(CompiledGraph workflow, Map<String, Object> inputs, String outputKey,
            String fallbackError, String threadId) {
        try {
            Optional<OverAllState> stateOpt;
            if (StringUtils.hasText(threadId)) {
                // 如果提供了会话ID，则写入RunnableConfig
                //将打包好的数据传给工作流
                stateOpt = workflow.invoke(inputs, RunnableConfig.builder().threadId(threadId.trim()).build());
            } else {
                // 如果未提供会话ID，则不写入RunnableConfig
                //将打包好的数据传给工作流
                stateOpt = workflow.invoke(inputs);
            }
            if (stateOpt.isEmpty()) {
                // 如果状态为空，则返回兜底文案
                return fallbackError;
            }
            Optional<Object> output = stateOpt.get().value(outputKey);
            if (output.isEmpty()) {
                // 如果输出为空，则返回兜底文案
                return fallbackError;
            }
            String text = output.get().toString();
            if (!StringUtils.hasText(text)) {
                // 如果输出为空，则返回兜底文案
                return fallbackError;
            }
            return text;
        } catch (Exception e) {
            // 如果执行过程中发生异常，则返回兜底文案
            return fallbackError;
        }
    }
}
