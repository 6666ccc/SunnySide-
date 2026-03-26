package cn.lc.sunnyside.Workflow.Health.nodes;

import cn.lc.sunnyside.Service.FamilyAccessService;
import cn.lc.sunnyside.Service.HealthRecordService;
import cn.lc.sunnyside.Workflow.common.WorkflowStateKeys;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Component
public class FetchHealthDataNode implements NodeAction {
    private final FamilyAccessService familyAccessService;
    private final HealthRecordService healthRecordService;

    /**
     * 构造健康数据获取节点。
     *
     * @param familyAccessService 家属访问控制服务
     * @param healthRecordService 健康记录查询服务
     */
    public FetchHealthDataNode(FamilyAccessService familyAccessService, HealthRecordService healthRecordService) {
        this.familyAccessService = familyAccessService;
        this.healthRecordService = healthRecordService;
    }

    /**
     * 根据已识别意图和家属上下文拉取老人健康数据。
     * 包含登录校验、绑定老人解析、日期解析与业务数据查询。
     *
     * @param state 当前工作流状态
     * @return 写入业务结果或错误信息的状态增量
     */
    @Override
    public Map<String, Object> apply(OverAllState state) {
        Map<String, Object> result = new HashMap<>();
        String intent = state.value(WorkflowStateKeys.INTENT).map(Object::toString).orElse("GENERAL");
        if (!"HEALTH_QUERY".equalsIgnoreCase(intent)) {
            result.put(WorkflowStateKeys.ERROR_CODE, "NOT_HEALTH_DOMAIN");
            result.put(WorkflowStateKeys.ERROR_MESSAGE, "当前请求不属于健康查询。");
            return result;
        }
        String phone = state.value(WorkflowStateKeys.FAMILY_PHONE).map(Object::toString).orElse(null);
        if (!StringUtils.hasText(phone)) {
            result.put(WorkflowStateKeys.ERROR_CODE, "FAMILY_NOT_LOGIN");
            result.put(WorkflowStateKeys.ERROR_MESSAGE, "未获取到家属手机号，请先登录。");
            return result;
        }

        Long elderId = familyAccessService.resolveDefaultElderId(phone);
        if (elderId == null) {
            result.put(WorkflowStateKeys.ERROR_CODE, "ELDER_NOT_BOUND");
            result.put(WorkflowStateKeys.ERROR_MESSAGE, "未找到您绑定的老人信息，请先确认绑定关系。");
            return result;
        }
        result.put(WorkflowStateKeys.ELDER_ID, elderId);

        String dateStr = state.value(WorkflowStateKeys.TARGET_DATE).map(Object::toString).orElse(null);
        LocalDate targetDate;
        try {
            targetDate = LocalDate.parse(dateStr);
        } catch (Exception e) {
            targetDate = LocalDate.now();
            result.put(WorkflowStateKeys.TARGET_DATE, targetDate.toString());
        }
        String healthData = healthRecordService.queryElderHealth(phone, elderId, targetDate, targetDate);
        result.put(WorkflowStateKeys.BIZ_RESULT, healthData);
        return result;
    }
}
