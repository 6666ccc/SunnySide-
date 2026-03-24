package cn.lc.sunnyside.Workflow;

import cn.lc.sunnyside.Workflow.Node.InputProcessNode;
import cn.lc.sunnyside.Workflow.Node.LLMProcessNode;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * SunnySide 核心工作流服务
 * 使用 Spring AI Alibaba Graph 的 StateGraph 构建和执行业务流程
 */
@Service
public class SunnySideWorkflowService {

    private final CompiledGraph workflow;

    /**
     * 构造函数中编排工作流节点与边
     *
     * @param inputNode 输入预处理节点
     * @param llmNode   LLM 处理节点
     */
    public SunnySideWorkflowService(InputProcessNode inputNode, LLMProcessNode llmNode) {
        // 1. 创建 StateGraph 实例
        StateGraph graph = new StateGraph();

        // 2. 将节点注册到图中，将 NodeAction 包装为 AsyncNodeAction
        try {
            graph.addNode("inputNode", AsyncNodeAction.node_async(inputNode));
            graph.addNode("llmNode", AsyncNodeAction.node_async(llmNode));
        } catch (Exception e) {
            throw new RuntimeException("构建工作流节点失败", e);
        }

        // 3. 定义图的边（流程的流转方向）
        // 从 START 节点流向 inputNode
        try {
            graph.addEdge(StateGraph.START, "inputNode");
            // 从 inputNode 流向 llmNode
            graph.addEdge("inputNode", "llmNode");
            // 从 llmNode 流向 END 节点，结束工作流
            graph.addEdge("llmNode", StateGraph.END);

            // 4. 编译成可执行的 CompiledGraph
            this.workflow = graph.compile();
        } catch (Exception e) {
            throw new RuntimeException("编译工作流图失败", e);
        }
    }

    /**
     * 执行工作流
     *
     * @param query 用户的输入文本
     * @return LLM 处理后的回复结果
     */
    public String executeWorkflow(String query) {
        // 构造初始状态所需的输入参数
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("query", query);

        // 调用 invoke 触发图的执行，并获取最终的状态结果
        Optional<OverAllState> stateOpt = workflow.invoke(inputs);

        if (stateOpt.isPresent()) {
            // 从返回的 OverAllState 中提取输出字段 "llm_response"
            return stateOpt.get().value("llm_response").map(Object::toString).orElse("");
        }
        return "";
    }
}
