package cn.lc.sunnyside.Workflow.Health;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * 节点1：意图分析节点
 * 专门使用大模型分析用户的输入，提取是否需要查询健康数据以及目标日期
 */
@Component
public class AnalyzeIntentNode implements NodeAction {
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AnalyzeIntentNode(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        // 从状态中获取用户输入
        String query = state.value("query").map(Object::toString).orElse("");
        Map<String, Object> result = new HashMap<>();

        // 构造专用的 Prompt 让大模型输出 JSON 格式
        String prompt = String.format(
                "请分析用户的输入，判断是否在询问老人的健康状况。\n" +
                        "请严格返回如下JSON格式，不要包含任何Markdown标记或其他说明文本：\n" +
                        "{\"is_health_query\": true/false, \"target_date\": \"yyyy-MM-dd\"}\n" +
                        "如果用户没有明确说明日期，默认使用今天（%s）。\n" +
                        "用户输入：%s",
                LocalDate.now().toString(), query);

        // 调用 ChatClient 生成回复
        String jsonResponse = chatClient.prompt()
                                        .user(prompt)
                                        .call()
                                        .content();

        try {
            // 清理可能的 markdown 代码块标记以便解析
            jsonResponse = jsonResponse.replace("```json", "").replace("```", "").trim();
            JsonNode jsonNode = objectMapper.readTree(jsonResponse);

            // 解析 JSON 字段
            JsonNode isHealthQueryNode = jsonNode.get("is_health_query");
            // 如果 JSON 中没有 is_health_query 字段，默认设为 false
            boolean isHealthQuery = isHealthQueryNode != null && isHealthQueryNode.asBoolean();

            // 解析 target_date 字段，默认使用当前日期
            JsonNode targetDateNode = jsonNode.get("target_date");
            String targetDate;
            if (targetDateNode != null && !targetDateNode.isNull()) {
                targetDate = targetDateNode.asText();
            } else {
                targetDate = LocalDate.now().toString();
            }

            // 将提取出来的结构化参数写入 State 传递给下一个节点
            result.put("is_health_query", isHealthQuery);
            result.put("target_date", targetDate);
        } catch (Exception e) {
            // 解析失败时，默认降级为非健康查询（走普通聊天流）
            result.put("is_health_query", false);
        }

        return result;
    }
}
