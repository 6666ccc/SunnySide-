package cn.lc.sunnyside.Workflow.Health;

import cn.lc.sunnyside.Workflow.Health.nodes.AnalyzeHealthIntentNode;
import cn.lc.sunnyside.Workflow.Health.nodes.FetchHealthDataNode;
import cn.lc.sunnyside.Workflow.Health.nodes.GenerateHealthReplyNode;
import cn.lc.sunnyside.Workflow.common.BaseWorkflowExecutor;
import cn.lc.sunnyside.Workflow.common.WorkflowStateKeys;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Service
public class HealthWorkflowService extends BaseWorkflowExecutor {

    private final CompiledGraph workflow;

    /**
     * 构建健康查询工作流图：意图分析 -> 数据获取 -> 回复生成。
     *
     * @param analyzeNode 健康意图分析节点
     * @param fetchNode   健康数据获取节点
     * @param replyNode   健康结果回复节点
     */
    public HealthWorkflowService(AnalyzeHealthIntentNode analyzeNode,
            FetchHealthDataNode fetchNode,
            GenerateHealthReplyNode replyNode) {
        StateGraph graph = new StateGraph();
        try {
            graph.addNode("analyzeNode", AsyncNodeAction.node_async(analyzeNode));
            graph.addNode("fetchNode", AsyncNodeAction.node_async(fetchNode));
            graph.addNode("replyNode", AsyncNodeAction.node_async(replyNode));
            graph.addEdge(StateGraph.START, "analyzeNode");
            graph.addEdge("analyzeNode", "fetchNode");
            graph.addEdge("fetchNode", "replyNode");
            graph.addEdge("replyNode", StateGraph.END);
            this.workflow = graph.compile();
        } catch (Exception e) {
            throw new RuntimeException("构建健康查询工作流失败", e);
        }
    }

    /**
     * 执行健康查询工作流（无会话ID版本）。
     *
     * @param query       用户问题
     * @param familyPhone 家属手机号
     * @return 工作流回复
     */
    public String executeWorkflow(String query, String familyPhone) {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put(WorkflowStateKeys.QUERY, query);
        if (StringUtils.hasText(familyPhone)) {
            inputs.put(WorkflowStateKeys.FAMILY_PHONE, familyPhone.trim());
        }
        return executeWorkflow(workflow, inputs, WorkflowStateKeys.FINAL_REPLY, "健康工作流执行失败。");
    }

    /**
     * 执行健康查询工作流（含会话ID版本）。
     *
     * @param query          用户问题
     * @param familyPhone    家属手机号
     * @param conversationId 会话ID
     * @return 工作流回复
     */
    public String executeWorkflow(String query, String familyPhone, String conversationId) {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put(WorkflowStateKeys.QUERY, query);
        if (StringUtils.hasText(familyPhone)) {
            inputs.put(WorkflowStateKeys.FAMILY_PHONE, familyPhone.trim());
        }
        if (StringUtils.hasText(conversationId)) {
            inputs.put(WorkflowStateKeys.CONVERSATION_ID, conversationId);
        }
        return executeWorkflow(workflow, inputs, WorkflowStateKeys.FINAL_REPLY, "健康工作流执行失败。");
    }
}
