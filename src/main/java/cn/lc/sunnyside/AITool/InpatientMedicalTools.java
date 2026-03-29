package cn.lc.sunnyside.AITool;

import cn.lc.sunnyside.POJO.DO.DietaryAdvice;
import cn.lc.sunnyside.POJO.DO.MedicalTeamDuty;
import cn.lc.sunnyside.POJO.DO.TreatmentPlan;
import cn.lc.sunnyside.POJO.DO.VitalSigns;
import cn.lc.sunnyside.Service.DietaryAdviceService;
import cn.lc.sunnyside.Service.MedicalTeamDutyService;
import cn.lc.sunnyside.Service.TreatmentPlanService;
import cn.lc.sunnyside.Service.VitalSignsService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class InpatientMedicalTools {

    private final VitalSignsService vitalSignsService;
    private final TreatmentPlanService treatmentPlanService;
    private final MedicalTeamDutyService medicalTeamDutyService;
    private final DietaryAdviceService dietaryAdviceService;

    @Tool(description = "查询患者最新生命体征，返回血压、心率、体温、血氧饱和度等。需要提供患者ID。")
    public String getLatestVitalSigns(
            @ToolParam(description = "患者ID") Long patientId) {
        if (patientId == null) {
            return "查询失败，请提供患者ID。";
        }
        List<VitalSigns> records = vitalSignsService.getLatestVitalSigns(patientId, 1);
        if (records == null || records.isEmpty()) {
            return "暂无该患者的生命体征记录。";
        }
        VitalSigns v = records.get(0);
        return "患者ID:" + patientId + " 最新体征"
                + " (记录时间:" + v.getRecordDate() + " " + v.getRecordTime() + ")"
                + " 血压:" + toText(v.getSystolicBp()) + "/" + toText(v.getDiastolicBp()) + "mmHg"
                + " 心率:" + toText(v.getHeartRate()) + "bpm"
                + " 体温:" + toText(v.getTemperature()) + "℃"
                + " 血氧:" + toText(v.getBloodOxygen()) + "%"
                + " 血糖:" + toText(v.getBloodSugar()) + "mmol/L"
                + " 记录护士:" + toText(v.getRecordedBy());
    }

    @Tool(description = "查询患者诊疗/护理计划，包括输液、手术、检查等安排。需要提供患者ID，日期可选（默认今天）。")
    public String getTreatmentPlans(
            @ToolParam(description = "患者ID") Long patientId,
            @ToolParam(description = "计划日期，格式yyyy-MM-dd，不填则默认今天") String date) {
        if (patientId == null) {
            return "查询失败，请提供患者ID。";
        }
        LocalDate planDate;
        try {
            planDate = (date != null && !date.isBlank()) ? LocalDate.parse(date.trim()) : LocalDate.now();
        } catch (DateTimeParseException e) {
            return "日期格式错误，请使用yyyy-MM-dd。";
        }
        List<TreatmentPlan> plans = treatmentPlanService.getTreatmentPlans(patientId, planDate);
        if (plans == null || plans.isEmpty()) {
            return "该患者在" + planDate + "无诊疗计划。";
        }
        return "患者ID:" + patientId + " " + planDate + "诊疗计划:\n"
                + plans.stream()
                .map(p -> "- " + p.getTaskName()
                        + " [" + p.getCategory() + "]"
                        + " " + p.getStartTime() + "~" + p.getEndTime()
                        + (p.getLocation() != null ? " 地点:" + p.getLocation() : "")
                        + (p.getDescription() != null ? " 备注:" + p.getDescription() : "")
                        + (Boolean.TRUE.equals(p.getIsCompleted()) ? " (已执行)" : " (待执行)"))
                .collect(Collectors.joining("\n"));
    }

    @Tool(description = "查询科室当日值班医疗团队，返回主治医生、责任护士等。需要提供科室ID。")
    public String getMedicalTeam(
            @ToolParam(description = "科室ID") Long deptId) {
        if (deptId == null) {
            return "查询失败，请提供科室ID。";
        }
        List<MedicalTeamDuty> staff = medicalTeamDutyService.getOnDutyStaff(deptId);
        if (staff == null || staff.isEmpty()) {
            return "该科室今日无值班记录。";
        }
        return "科室ID:" + deptId + " 今日值班团队:\n"
                + staff.stream()
                .map(s -> "- " + s.getStaffName()
                        + " [" + s.getStaffRole() + "]"
                        + (s.getDutyTime() != null ? " 班次:" + s.getDutyTime() : "")
                        + (s.getPhone() != null ? " 电话:" + s.getPhone() : ""))
                .collect(Collectors.joining("\n"));
    }

    @Tool(description = "查询患者饮食医嘱及禁忌，返回餐次、饮食内容和营养禁忌。需要提供患者ID。")
    public String getDietaryAdvice(
            @ToolParam(description = "患者ID") Long patientId) {
        if (patientId == null) {
            return "查询失败，请提供患者ID。";
        }
        List<DietaryAdvice> advices = dietaryAdviceService.getDietaryAdvice(patientId, LocalDate.now());
        if (advices == null || advices.isEmpty()) {
            return "该患者今日无饮食安排记录。";
        }
        return "患者ID:" + patientId + " 今日饮食安排:\n"
                + advices.stream()
                .map(a -> "- [" + a.getMealType() + "] " + a.getFoodContent()
                        + (a.getNutritionNotes() != null ? " (医嘱禁忌:" + a.getNutritionNotes() + ")" : ""))
                .collect(Collectors.joining("\n"));
    }

    private String toText(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }
}
