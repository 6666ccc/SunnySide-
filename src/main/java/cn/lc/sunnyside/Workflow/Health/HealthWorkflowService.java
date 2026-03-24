package cn.lc.sunnyside.Workflow.Health;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 家属查询老人健康状况专用的 Workflow 编排服务
 */
@Service
public class HealthWorkflowService {

    private final CompiledGraph workflow;


    public HealthWorkflowService(AnalyzeIntentNode analyzeNode,
            FetchHealthDataNode fetchNode,
            GenerateReplyNode replyNode) {
        // 1. 初始化图结构
        StateGraph graph = new StateGraph();
        try {
            // 2. 注册节点
            graph.addNode("analyzeNode", AsyncNodeAction.node_async(analyzeNode));
            graph.addNode("fetchNode", AsyncNodeAction.node_async(fetchNode));
            graph.addNode("replyNode", AsyncNodeAction.node_async(replyNode));

            // 3. 编排边（定义执行的先后顺序：一条标准作业流水线 SOP）
            // 流转：START -> analyzeNode -> fetchNode -> replyNode -> END
            graph.addEdge(StateGraph.START, "analyzeNode");
            graph.addEdge("analyzeNode", "fetchNode");
            graph.addEdge("fetchNode", "replyNode");
            graph.addEdge("replyNode", StateGraph.END);

            // 4. 编译工作流
            this.workflow = graph.compile();
        } catch (Exception e) {
            throw new RuntimeException("构建健康查询工作流失败", e);
        }
    }

    /**
     * 触发并执行工作流
     * 
     * @param query       用户输入
     * @param familyPhone 家属手机号（作为上下文传入 State）
     * @return 最终回复
     */
    public String executeWorkflow(String query, String familyPhone) {
        // 构造初始状态（State）
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("query", query);
        if (familyPhone != null && !familyPhone.isBlank()) {
            inputs.put("familyPhone", familyPhone);
        }

        // 调用 invoke 执行完整的流程图
        Optional<OverAllState> stateOpt = workflow.invoke(inputs);

        if (stateOpt.isPresent()) {
            // 从返回的 OverAllState 中提取输出字段 "final_reply"
            OverAllState state = stateOpt.get();
            Optional<Object> finalReplyOpt = state.value("final_reply");

            if (finalReplyOpt.isPresent()) {
                Object finalReplyObj = finalReplyOpt.get();
                return finalReplyObj.toString();
            } else {
                return "工作流执行异常，无返回结果。";
            }
        }
        return "工作流执行失败。";
    }
}
