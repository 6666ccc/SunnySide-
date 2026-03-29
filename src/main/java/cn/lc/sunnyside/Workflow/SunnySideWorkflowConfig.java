package cn.lc.sunnyside.Workflow;

import cn.lc.sunnyside.Workflow.common.WorkflowGraphSupport;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 住院医疗工作流配置：RouterNode（LLM + Tool Calling）-> ResponseNode（生成最终回复）。
 */
@Configuration
public class SunnySideWorkflowConfig {

    public static final String SUNNY_SIDE_DEMO_WORKFLOW_BEAN = "sunnySideDemoWorkflow";
    public static final String MEDICAL_WORKFLOW_BEAN = "medicalWorkflow";

    @Bean(name = SUNNY_SIDE_DEMO_WORKFLOW_BEAN)
    public CompiledGraph sunnySideDemoWorkflow(
            SunnySideInputProcessNode inputNode,
            SunnySideLlmProcessNode llmNode) {
        StateGraph graph = new StateGraph(WorkflowGraphSupport.replaceKeyStrategyFactory());
        try {
            graph.addNode("inputNode", AsyncNodeAction.node_async(inputNode));
            graph.addNode("llmNode", AsyncNodeAction.node_async(llmNode));
            graph.addEdge(StateGraph.START, "inputNode");
            graph.addEdge("inputNode", "llmNode");
            graph.addEdge("llmNode", StateGraph.END);
            return WorkflowGraphSupport.compile(graph);
        } catch (GraphStateException e) {
            throw new IllegalStateException("编译 SunnySide 演示工作流失败", e);
        }
    }

    @Bean(name = MEDICAL_WORKFLOW_BEAN)
    public CompiledGraph medicalWorkflow(
            MedicalRouterNode routerNode,
            MedicalResponseNode responseNode) {
        StateGraph graph = new StateGraph(WorkflowGraphSupport.replaceKeyStrategyFactory());
        try {
            graph.addNode("routerNode", AsyncNodeAction.node_async(routerNode));
            graph.addNode("responseNode", AsyncNodeAction.node_async(responseNode));
            graph.addEdge(StateGraph.START, "routerNode");
            graph.addEdge("routerNode", "responseNode");
            graph.addEdge("responseNode", StateGraph.END);
            return WorkflowGraphSupport.compile(graph);
        } catch (GraphStateException e) {
            throw new IllegalStateException("编译住院医疗工作流失败", e);
        }
    }
}
