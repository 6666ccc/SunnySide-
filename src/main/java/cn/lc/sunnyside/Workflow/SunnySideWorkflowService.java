package cn.lc.sunnyside.Workflow;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;
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
     * 输入预处理节点
     */
    @Component
    public static class InputProcessNode implements NodeAction {
        @Override
        public Map<String, Object> apply(OverAllState state) throws Exception {
            String input = state.value("query").map(Object::toString).orElse("");
            Map<String, Object> result = new HashMap<>();
            result.put("processed_query", input.trim());
            return result;
        }
    }

    /**
     * LLM 处理节点
     */
    @Component
    public static class LLMProcessNode implements NodeAction {
        private final ChatClient chatClient;

        public LLMProcessNode(ChatClient.Builder builder) {
            this.chatClient = builder.build();
        }

        @Override
        public Map<String, Object> apply(OverAllState state) throws Exception {
            String query = state.value("processed_query").map(Object::toString).orElse("");
            String answer = query.isEmpty() ? "请输入您的问题。" : chatClient.prompt().user(query).call().content();
            Map<String, Object> result = new HashMap<>();
            result.put("llm_response", answer);
            return result;
        }
    }

    /**
     * 构造函数中编排工作流节点与边
     */
    public SunnySideWorkflowService(InputProcessNode inputNode, LLMProcessNode llmNode) {
        StateGraph graph = new StateGraph();
        try {
            graph.addNode("inputNode", AsyncNodeAction.node_async(inputNode));
            graph.addNode("llmNode", AsyncNodeAction.node_async(llmNode));
            graph.addEdge(StateGraph.START, "inputNode");
            graph.addEdge("inputNode", "llmNode");
            graph.addEdge("llmNode", StateGraph.END);
            this.workflow = graph.compile();
        } catch (Exception e) {
            throw new RuntimeException("构建工作流失败", e);
        }
    }

    /**
     * 执行工作流
     */
    public String executeWorkflow(String query) {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("query", query);
        return workflow.invoke(inputs)
                .flatMap(state -> state.value("llm_response"))
                .map(Object::toString)
                .orElse("");
    }
}
