package cn.lc.sunnyside.Workflow.Health;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 节点3：最终回复生成节点
 * 结合用户的原始问题和上一步查出的真实健康数据，让大模型生成带有情感关怀的回复。
 */
@Component
public class GenerateReplyNode implements NodeAction {
    private final ChatClient chatClient;

    public GenerateReplyNode(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        Map<String, Object> result = new HashMap<>();
        //从state中获取query和error
        String query = state.value("query").map(Object::toString).orElse("");
        String error = state.value("error").map(Object::toString).orElse(null);

        // 如果之前的节点（如权限校验）抛出了明确错误，直接返回该错误，终止生成
        if (error != null) {
            result.put("final_reply", error);
            return result;
        }

        // 从 State 中获取是否是健康查询的标志位
        boolean isHealthQuery = state.value("is_health_query").map(o -> (Boolean) o).orElse(false);

        String answer;
        if (isHealthQuery) {
            // 从 State 中获取健康数据
            String healthData = state.value("health_data").map(Object::toString).orElse("");

            // 组装专门用于回复生成的 Prompt
            String prompt = String.format(
                    "你是一个养老院的专属健康助手，语气要温柔、关切、专业。\n" +
                            "请根据以下由系统查询到的老人真实健康数据，回答家属的提问。\n\n" +
                            "家属提问：%s\n" +
                            "健康数据：%s\n\n" +
                            "请直接给出回复，不要说多余的废话，不要暴露系统查询的底层细节。",
                    query, healthData);
            answer = chatClient.prompt().user(prompt).call().content();
        } else {
            // 如果不是健康查询，退化为普通聊天（例如只是问好）
            answer = chatClient.prompt().user(query).call().content();
        }

        // 将最终答案写入 State
        result.put("final_reply", answer);
        return result;
    }
}
