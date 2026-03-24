package cn.lc.sunnyside.Workflow.Health;

import cn.lc.sunnyside.Service.FamilyAccessService;
import cn.lc.sunnyside.Service.HealthRecordService;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * 节点2：健康数据查询节点（纯 Java 业务逻辑，不涉及大模型）
 * 根据上一个节点解析出的参数，执行严格的数据库查询
 */
@Component
public class FetchHealthDataNode implements NodeAction {
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

        // 如果上一个节点判断为【查健康】，则执行查询逻辑
        if (isHealthQuery) {
            String phone = state.value("familyPhone").map(Object::toString).orElse(null);
            if (phone == null || phone.isBlank()) {
                result.put("error", "未获取到家属手机号，请先登录或提供手机号。");
                return result; // 写入错误信息并结束当前节点逻辑
            }

            // 1. 校验绑定权限并获取老人ID
            Long elderId = familyAccessService.resolveDefaultElderId(phone);
            if (elderId == null) {
                result.put("error", "未找到您绑定的老人信息，请确认绑定关系。");
                return result;
            }

            // 2. 解析时间
            String dateStr = state.value("target_date").map(Object::toString).orElse(null);
            LocalDate targetDate;
            try {
                targetDate = LocalDate.parse(dateStr);
            } catch (Exception e) {
                targetDate = LocalDate.now();
            }

            // 3. 查询真实的健康记录服务
            String healthData = healthRecordService.queryElderHealth(phone, elderId, targetDate, targetDate);

            // 4. 将查询到的硬核数据放入 State
            result.put("health_data", healthData);
        }

        return result;
    }
}
