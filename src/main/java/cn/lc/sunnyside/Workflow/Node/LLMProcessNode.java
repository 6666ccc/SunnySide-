package cn.lc.sunnyside.Workflow.Node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * LLM 处理节点
 * 负责调用 Spring AI 的 ChatClient 生成针对用户输入的回复。
 */
@Component
public class LLMProcessNode implements NodeAction {

    private final ChatClient chatClient;

    /**
     * 构造函数注入 ChatClient.Builder
     * 
     * @param builder Spring AI 提供的 ChatClient 构建器
     */
    public LLMProcessNode(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        // 1. 从状态中获取上一节点处理后的输入
        String query = state.value("processed_query").map(Object::toString).orElse("");

        // 2. 调用 ChatClient 生成回复
        // 如果输入为空，给出默认回复
        String answer;
        if (query.isEmpty()) {
            answer = "请输入您的问题。";
        } else {
            answer = chatClient.prompt()
                    .user(query)
                    .call()
                    .content();
        }

        // 3. 将生成的回复放入更新状态字典中
        // 约定输出结果存入 "llm_response" 键
        Map<String, Object> result = new HashMap<>();
        result.put("llm_response", answer);

        return result;
    }
}
