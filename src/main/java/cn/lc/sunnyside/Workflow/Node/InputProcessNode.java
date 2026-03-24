package cn.lc.sunnyside.Workflow.Node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 输入预处理节点
 * 负责从 OverAllState 中提取用户输入，并进行规范化处理（例如去除两端空格）。
 */
@Component
public class InputProcessNode implements NodeAction {

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        // 1. 从状态中获取原始输入
        // 约定外部传入的键为 "query"
        String input = state.value("query").map(Object::toString).orElse("");

        // 2. 规范化处理：去除前后空格
        String processedInput = input.trim();

        // 3. 将处理后的结果存入状态，返回更新后的状态字典
        Map<String, Object> result = new HashMap<>();
        result.put("processed_query", processedInput);
        
        return result;
    }
}
