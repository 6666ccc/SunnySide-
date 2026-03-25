package cn.lc.sunnyside.Workflow.Health;

import cn.lc.sunnyside.Service.FamilyAccessService;
import cn.lc.sunnyside.Service.HealthRecordService;
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

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 家属查询老人健康状况专用的 Workflow 编排服务
 */
@Service
public class HealthWorkflowService {

    private final CompiledGraph workflow;

    @Component
    public static class AnalyzeIntentNode implements NodeAction {
        private final ChatClient chatClient;
        private final ObjectMapper objectMapper = new ObjectMapper();

        public AnalyzeIntentNode(ChatClient.Builder builder) {
            this.chatClient = builder.build();
        }

        @Override
        public Map<String, Object> apply(OverAllState state) throws Exception {
            String query = state.value("query").map(Object::toString).orElse("");
            Map<String, Object> result = new HashMap<>();

            String prompt = String.format(
                    "请分析用户的输入，判断是否在询问老人的健康状况。\n" +
                            "请严格返回如下JSON格式，不要包含任何Markdown标记或其他说明文本：\n" +
                            "{\"is_health_query\": true/false, \"target_date\": \"yyyy-MM-dd\"}\n" +
                            "如果用户没有明确说明日期，默认使用今天（%s）。\n" +
                            "用户输入：%s",
                    LocalDate.now().toString(), query);

            String jsonResponse = chatClient.prompt().user(prompt).call().content();

            try {
                jsonResponse = jsonResponse.replace("```json", "").replace("```", "").trim();
                JsonNode jsonNode = objectMapper.readTree(jsonResponse);
                JsonNode isHealthQueryNode = jsonNode.get("is_health_query");
                boolean isHealthQuery = isHealthQueryNode != null && isHealthQueryNode.asBoolean();
                JsonNode targetDateNode = jsonNode.get("target_date");
                String targetDate = (targetDateNode != null && !targetDateNode.isNull()) ? targetDateNode.asText()
                        : LocalDate.now().toString();

                result.put("is_health_query", isHealthQuery);
                result.put("target_date", targetDate);
            } catch (Exception e) {
                result.put("is_health_query", false);
            }
            return result;
        }
    }

    @Component
    public static class FetchHealthDataNode implements NodeAction {
        private final FamilyAccessService familyAccessService;
        private final HealthRecordService healthRecordService;

        public FetchHealthDataNode(FamilyAccessService familyAccessService, HealthRecordService healthRecordService) {
            this.familyAccessService = familyAccessService;
            this.healthRecordService = healthRecordService;
        }

        @Override
        public Map<String, Object> apply(OverAllState state) throws Exception {
            Map<String, Object> result = new HashMap<>();
            boolean isHealthQuery = state.value("is_health_query").map(o -> (Boolean) o).orElse(false);

            if (isHealthQuery) {
                String phone = state.value("familyPhone").map(Object::toString).orElse(null);
                if (phone == null || phone.isBlank()) {
                    result.put("error", "未获取到家属手机号，请先登录或提供手机号。");
                    return result;
                }

                Long elderId = familyAccessService.resolveDefaultElderId(phone);
                if (elderId == null) {
                    result.put("error", "未找到您绑定的老人信息，请确认绑定关系。");
                    return result;
                }

                String dateStr = state.value("target_date").map(Object::toString).orElse(null);
                LocalDate targetDate;
                try {
                    targetDate = LocalDate.parse(dateStr);
                } catch (Exception e) {
                    targetDate = LocalDate.now();
                }

                String healthData = healthRecordService.queryElderHealth(phone, elderId, targetDate, targetDate);
                result.put("health_data", healthData);
            }
            return result;
        }
    }

    @Component
    public static class GenerateReplyNode implements NodeAction {
        private final ChatClient chatClient;

        public GenerateReplyNode(ChatClient.Builder builder) {
            this.chatClient = builder.build();
        }

        @Override
        public Map<String, Object> apply(OverAllState state) throws Exception {
            Map<String, Object> result = new HashMap<>();
            String query = state.value("query").map(Object::toString).orElse("");
            String error = state.value("error").map(Object::toString).orElse(null);

            if (error != null) {
                result.put("final_reply", error);
                return result;
            }

            boolean isHealthQuery = state.value("is_health_query").map(o -> (Boolean) o).orElse(false);
            String answer;

            if (isHealthQuery) {
                String healthData = state.value("health_data").map(Object::toString).orElse("");
                String prompt = String.format(
                        "你是一个养老院的专属健康助手，语气要温柔、关切、专业。\n" +
                                "请根据以下由系统查询到的老人真实健康数据，回答家属的提问。\n\n" +
                                "家属提问：%s\n" +
                                "健康数据：%s\n\n" +
                                "请直接给出回复，不要说多余的废话，不要暴露系统查询的底层细节。",
                        query, healthData);
                answer = chatClient.prompt().user(prompt).call().content();
            } else {
                answer = chatClient.prompt().user(query).call().content();
            }

            result.put("final_reply", answer);
            return result;
        }
    }

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
