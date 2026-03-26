package cn.lc.sunnyside.Workflow.Health.nodes;

import cn.lc.sunnyside.Workflow.common.WorkflowStateKeys;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Component
public class AnalyzeHealthIntentNode implements NodeAction {
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 构造健康意图分析节点。
     *
     * @param builder ChatClient 构造器
     */
    public AnalyzeHealthIntentNode(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    /**
     * 分析用户问题是否属于健康查询，并抽取目标日期。
     * 优先用模型结构化判断，失败时回退关键字规则。
     *
     * @param state 当前工作流状态
     * @return 写入意图、操作类型与目标日期的状态增量
     */
    @Override
    public Map<String, Object> apply(OverAllState state) {
        String query = state.value(WorkflowStateKeys.QUERY).map(Object::toString).orElse("");
        Map<String, Object> result = new HashMap<>();

        String prompt = String.format(
                "请分析用户输入是否属于家属查询老人健康数据。\n" +
                        "返回JSON：{\"is_health_query\":true/false,\"target_date\":\"yyyy-MM-dd\"}\n" +
                        "若用户未提及日期，target_date使用今天（%s）。\n" +
                        "用户输入：%s",
                LocalDate.now().toString(), query);

        try {
            String jsonResponse = chatClient.prompt().user(prompt).call().content();
            jsonResponse = jsonResponse.replace("```json", "").replace("```", "").trim();
            JsonNode jsonNode = objectMapper.readTree(jsonResponse);
            JsonNode isHealthQueryNode = jsonNode.get("is_health_query");
            boolean isHealthQuery = isHealthQueryNode != null && isHealthQueryNode.asBoolean();
            JsonNode targetDateNode = jsonNode.get("target_date");
            String targetDate = (targetDateNode != null && !targetDateNode.isNull()) ? targetDateNode.asText()
                    : LocalDate.now().toString();
            result.put(WorkflowStateKeys.INTENT, isHealthQuery ? "HEALTH_QUERY" : "GENERAL");
            result.put(WorkflowStateKeys.OPERATION, "QUERY");
            result.put(WorkflowStateKeys.TARGET_DATE, targetDate);
        } catch (Exception e) {
            boolean keywordMatched = query.contains("健康")
                    || query.contains("血压")
                    || query.contains("体温")
                    || query.contains("心率")
                    || query.contains("血糖");
            result.put(WorkflowStateKeys.INTENT, keywordMatched ? "HEALTH_QUERY" : "GENERAL");
            result.put(WorkflowStateKeys.OPERATION, "QUERY");
            result.put(WorkflowStateKeys.TARGET_DATE, LocalDate.now().toString());
        }
        return result;
    }
}
