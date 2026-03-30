package cn.lc.sunnyside.Workflow;

import cn.lc.sunnyside.Workflow.common.WorkflowGraphSupport;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 本类是定义工作流的配置类,用于定义工作流的节点和边同时也是workflow核心,主要功能是告诉程序如何加工数据什么时候该干什么下一步该干什么加工成什么样子需要调用什么工具等.
 */
@Configuration
public class SunnySideWorkflowConfig {

    public static final String MEDICAL_WORKFLOW_BEAN = "medicalWorkflow";

    //定义住院医疗工作流
    @Bean(name = MEDICAL_WORKFLOW_BEAN)
    public CompiledGraph medicalWorkflow(
            MedicalRouterNode routerNode,
            MedicalResponseNode responseNode) {
        //创建一个StateGraph实例用于构建工作流图,传入的参数是WorkflowGraphSupport.replaceKeyStrategyFactory()用于构建全局Map
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
