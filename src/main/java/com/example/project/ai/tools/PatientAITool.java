package com.example.project.ai.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.project.service.InpatientAiDataService;

/**
 * 面向「患者本人登录」账号的 Spring AI 工具：患者主键由当前 JWT 在服务端解析，无需模型猜测 patientId。
 */
@Component
public class PatientAITool {

    @Autowired
    private InpatientAiDataService inpatientAiDataService;

    @Tool(description = """
            【首选】根据当前 HTTP 请求头中的 JWT（Authorization: Bearer，subject 为患者端登录用户名）解析患者身份，
            返回 patientId、loginUsername 与在院基础信息（科室、床号、入院日期等）的 JSON。
            对话开始或用户问「我是谁/我的床位/哪个科」时应优先调用；须与患者登录接口签发的令牌在同一请求上下文中使用。无参数。""")
    public String getCurrentPatientContextFromJwt() {
        return inpatientAiDataService.getCurrentPatientContextFromJwt();
    }

    @Tool(description = """
            按日期查询「本人」的诊疗与护理计划。参数 planDate：YYYY-MM-DD，空则按服务端当日。
            对应数据：treatment_plan。""")
    public String queryMyTreatmentPlanByDate(String planDate) {
        return inpatientAiDataService.queryMyTreatmentPlanByDate(planDate);
    }

    @Tool(description = """
            查询「本人」在指定日期的生命体征。参数 recordDate：YYYY-MM-DD，空则按服务端当日。
            对应数据：vital_signs。""")
    public String queryMyVitalSigns(String recordDate) {
        return inpatientAiDataService.queryMyVitalSigns(recordDate);
    }

    @Tool(description = """
            查询「本人」近期（约两周）非用餐类诊疗与检查安排摘要。
            对应数据：treatment_plan。""")
    public String queryMyTreatmentAndDetection() {
        return inpatientAiDataService.queryMyTreatmentAndDetection();
    }

    @Tool(description = """
            查询「本人」近若干天生命体征趋势。参数 days：正整数，缺省 7，最大 90。""")
    public String queryMyVitalSignsTrend(String days) {
        return inpatientAiDataService.queryMyVitalSignsTrend(days);
    }

    @Tool(description = """
            查询「本人」医嘱相关的饮食安排。参数 mealDate：YYYY-MM-DD，空则按服务端当日。
            对应数据：dietary_advice。""")
    public String queryMyDietaryAdvice(String mealDate) {
        return inpatientAiDataService.queryMyDietaryAdvice(mealDate);
    }

    @Tool(description = """
            汇总「本人」今日行程：计划、饮食医嘱、所在科室当日医护值班（服务端当日）。""")
    public String queryMyTodayScheduleSummary() {
        return inpatientAiDataService.queryMyTodayScheduleSummary();
    }

    @Tool(description = """
            「本人」出院准备相关只读摘要：在院状态与未完成非用餐类计划项等。""")
    public String queryMyDischargeProgress() {
        return inpatientAiDataService.queryMyDischargeProgress();
    }

    @Tool(description = """
            查询「本人」所在科室/病区的公开信息（护士站电话、位置等）。无参数。""")
    public String queryMyDepartmentInfo() {
        return inpatientAiDataService.queryMyDepartmentInfo();
    }

    @Tool(description = """
            查询「本人」所在科室在指定日期的医护值班。参数 dutyDate：YYYY-MM-DD，空则按服务端当日。""")
    public String queryMyMedicalTeamDuty(String dutyDate) {
        return inpatientAiDataService.queryMyMedicalTeamDuty(dutyDate);
    }

    @Tool(description = """
            查询医院/科室公告。参数 publishDateSince：可选，YYYY-MM-DD。""")
    public String queryMyHospitalAnnouncements(String publishDateSince) {
        return inpatientAiDataService.queryMyHospitalAnnouncements(publishDateSince);
    }

    @Tool(description = """
            查询「本人」所在科室周边的便民设施（食堂、停车等）。无参数。""")
    public String queryMyNearbyFacilities() {
        return inpatientAiDataService.queryMyNearbyFacilities();
    }

    @Tool(description = """
            查询住院常见问答（FAQ）。参数 category：可选，如 ADMISSION、EXPENSE、INSURANCE、DISCHARGE、GENERAL；留空表示全部。""")
    public String queryFrequentlyAskedQuestions(String category) {
        return inpatientAiDataService.queryFrequentlyAskedQuestions(category);
    }
}
