package cn.lc.sunnyside.Workflow;

import cn.lc.sunnyside.Workflow.Health.HealthWorkflowService;
import cn.lc.sunnyside.Workflow.Visit.VisitWorkflowService;
import cn.lc.sunnyside.Workflow.common.BaseWorkflowExecutor;
import cn.lc.sunnyside.Workflow.common.WorkflowStateKeys;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Service
public class AgentRouterWorkflowService extends BaseWorkflowExecutor {

    private final CompiledGraph workflow;

    @Component
    public static class AnalyzeRouteNode implements NodeAction {
        private final ChatClient chatClient;
        private final ObjectMapper objectMapper = new ObjectMapper();

        /**
         * 构造路由分析节点。
         *
         * @param builder ChatClient 构造器
         */
        public AnalyzeRouteNode(ChatClient.Builder builder) {
            this.chatClient = builder.build();
        }

        /**
         * 分析当前问题所属业务域。
         * 先走关键字快速判定，未命中再使用模型输出结构化路由结果。
         *
         * @param state 当前工作流状态
         * @return 写入 DOMAIN 字段的状态增量
         */
        @Override
        public Map<String, Object> apply(OverAllState state) {
            String query = state.value(WorkflowStateKeys.QUERY).map(Object::toString).orElse("");
            Map<String, Object> result = new HashMap<>();
            String domainByKeyword = keywordDomain(query);
            if (!"GENERAL".equals(domainByKeyword)) {
                result.put(WorkflowStateKeys.DOMAIN, domainByKeyword);
                return result;
            }

            String prompt = "请识别用户问题所属业务域并仅输出JSON：{\"domain\":\"HEALTH|VISIT|GENERAL\"}。\n用户输入：" + query;
            try {
                String json = chatClient.prompt().user(prompt).call().content();
                json = json.replace("```json", "").replace("```", "").trim();
                JsonNode node = objectMapper.readTree(json);
                String domain = node.path("domain").asText("GENERAL");
                if (!"HEALTH".equalsIgnoreCase(domain) && !"VISIT".equalsIgnoreCase(domain)) {
                    domain = "GENERAL";
                }
                result.put(WorkflowStateKeys.DOMAIN, domain.toUpperCase());
            } catch (Exception e) {
                result.put(WorkflowStateKeys.DOMAIN, "GENERAL");
            }
            return result;
        }

        /**
         * 基于关键字进行轻量业务域判断。
         *
         * @param query 用户问题
         * @return HEALTH、VISIT 或 GENERAL
         */
        private String keywordDomain(String query) {
            if (query.contains("健康") || query.contains("血压") || query.contains("体温") || query.contains("心率")
                    || query.contains("血糖")) {
                return "HEALTH";
            }
            if (query.contains("探访") || query.contains("看望") || query.contains("来访") || query.contains("预约")) {
                return "VISIT";
            }
            return "GENERAL";
        }
    }

    @Component
    public static class DispatchNode implements NodeAction {
        private final HealthWorkflowService healthWorkflowService;
        private final VisitWorkflowService visitWorkflowService;

        /**
         * 构造分发节点。
         *
         * @param healthWorkflowService 健康工作流服务
         * @param visitWorkflowService  探访工作流服务
         */
        public DispatchNode(HealthWorkflowService healthWorkflowService, VisitWorkflowService visitWorkflowService) {
            this.healthWorkflowService = healthWorkflowService;
            this.visitWorkflowService = visitWorkflowService;
        }

        /**
         * 根据业务域将请求分发到对应子工作流并回填统一回复字段。
         *
         * @param state 当前工作流状态
         * @return 写入 FINAL_REPLY 字段的状态增量
         */
        @Override
        public Map<String, Object> apply(OverAllState state) {
            Map<String, Object> result = new HashMap<>();
            String domain = state.value(WorkflowStateKeys.DOMAIN).map(Object::toString).orElse("GENERAL");
            String query = state.value(WorkflowStateKeys.QUERY).map(Object::toString).orElse("");
            String familyPhone = state.value(WorkflowStateKeys.FAMILY_PHONE).map(Object::toString).orElse(null);
            String conversationId = state.value(WorkflowStateKeys.CONVERSATION_ID).map(Object::toString).orElse(null);

            String reply = "";
            if ("HEALTH".equalsIgnoreCase(domain)) {
                reply = healthWorkflowService.executeWorkflow(query, familyPhone, conversationId);
            } else if ("VISIT".equalsIgnoreCase(domain)) {
                reply = visitWorkflowService.executeWorkflow(query, familyPhone, conversationId);
            }
            result.put(WorkflowStateKeys.FINAL_REPLY, reply);
            return result;
        }
    }

    /**
     * 构建总控路由工作流图：路由分析 -> 业务分发。
     *
     * @param analyzeRouteNode 路由分析节点
     * @param dispatchNode     分发执行节点
     */
    public AgentRouterWorkflowService(AnalyzeRouteNode analyzeRouteNode, DispatchNode dispatchNode) {
        StateGraph graph = new StateGraph();
        try {
            graph.addNode("analyzeRouteNode", AsyncNodeAction.node_async(analyzeRouteNode));
            graph.addNode("dispatchNode", AsyncNodeAction.node_async(dispatchNode));
            graph.addEdge(StateGraph.START, "analyzeRouteNode");
            graph.addEdge("analyzeRouteNode", "dispatchNode");
            graph.addEdge("dispatchNode", StateGraph.END);
            this.workflow = graph.compile();
        } catch (Exception e) {
            throw new RuntimeException("构建总控路由工作流失败", e);
        }
    }

    /**
     * 执行总控路由工作流。
     *
     * @param query          用户问题
     * @param familyPhone    家属手机号
     * @param conversationId 会话ID
     * @return 子工作流生成的最终回复；无命中时返回空字符串
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
        return executeWorkflow(workflow, inputs, WorkflowStateKeys.FINAL_REPLY, "");
    }
}
