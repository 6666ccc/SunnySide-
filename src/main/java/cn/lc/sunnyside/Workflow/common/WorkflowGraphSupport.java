package cn.lc.sunnyside.Workflow.common;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;

import java.util.HashMap;

public final class WorkflowGraphSupport {

    private static final ReplaceStrategy REPLACE = new ReplaceStrategy();

    private WorkflowGraphSupport() {
    }

    /**
     * 默认的 KeyStrategyFactory，针对核心工作流状态键使用 ReplaceStrategy，确保每次执行都覆盖之前的值。
     */

    public static KeyStrategyFactory replaceKeyStrategyFactory() {
        return () -> {
            HashMap<String, KeyStrategy> strategies = new HashMap<>();
            putReplace(strategies, WorkflowStateKeys.QUERY);
            putReplace(strategies, WorkflowStateKeys.CONVERSATION_ID);
            putReplace(strategies, WorkflowStateKeys.PATIENT_ID);
            putReplace(strategies, WorkflowStateKeys.TOOL_CONTEXT);
            putReplace(strategies, WorkflowStateKeys.FINAL_REPLY);
            putReplace(strategies, WorkflowStateKeys.RELATIVE_PHONE);
            putReplace(strategies, WorkflowStateKeys.PROCESSED_QUERY);
            putReplace(strategies, WorkflowStateKeys.LLM_RESPONSE);
            putReplace(strategies, WorkflowStateKeys.CARE_ROUTE);
            return strategies;
        };
    }

    //帮助方法：将指定键与 ReplaceStrategy 关联
    private static void putReplace(HashMap<String, KeyStrategy> strategies, String key) {
        strategies.put(key, REPLACE);
    }

    // 构建默认的 CompileConfig，注入自定义的 KeyStrategyFactory
    public static CompileConfig defaultCompileConfig() {
        return CompileConfig.builder().build();
    }

    // 编译 StateGraph，使用默认的 CompileConfig
    public static CompiledGraph compile(StateGraph graph) throws GraphStateException {
        return graph.compile(defaultCompileConfig());
    }
}
