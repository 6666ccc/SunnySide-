package cn.lc.sunnyside.Workflow.common;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Optional;

/**
 * 工作流执行基类。
 * 统一封装图执行、输出读取与兜底错误处理逻辑。
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
        try {
            Optional<OverAllState> stateOpt = workflow.invoke(inputs);
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
