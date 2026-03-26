package cn.lc.sunnyside.AITool;

import cn.lc.sunnyside.Auth.FamilyLoginContext;
import cn.lc.sunnyside.POJO.DO.*;
import cn.lc.sunnyside.Service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 亲属使用的AI工具
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RelativesTool {

    private final VisitAppointmentService visitAppointmentService;
    private final FamilyAccessService familyAccessService;
    private final HealthRecordService healthRecordService;
    private final ElderIdentityHelper elderIdentityHelper;

    @Tool(description = "预约看望工具。输入访客信息，必须基于当前登录家属绑定的老人。")
    public String bookVisit(
            @ToolParam(description = "老人姓名。如果有多个绑定老人，可通过姓名区分，不填则查默认老人。") String elderName,
            @ToolParam(description = "访客姓名。") String visitorName,
            @ToolParam(description = "来访时间（ISO格式，例如2023-10-01T10:00:00）。") String time,
            @ToolParam(description = "关系。") String relation,
            @ToolParam(description = "联系电话。") String phone) {
        String resolvedFamilyPhone = resolveFamilyPhone();
        Long resolvedElderId = resolveElderIdForFamily(elderName, resolvedFamilyPhone);
        if (resolvedElderId == null) {
            return "预约失败，" + unresolvedFamilyElderMessage(resolvedFamilyPhone);
        }
        log.info("调用工具[bookVisit]，elderId={}，visitorName={}，time={}，relation={}，phone={}",
                resolvedElderId, visitorName, time, relation, phone);
        try {
            LocalDateTime visitTime = LocalDateTime.parse(time);
            String result = visitAppointmentService.bookVisit(resolvedElderId, visitorName, phone, visitTime, relation);
            log.info("工具[bookVisit]预约结果={}", result);
            return result;
        } catch (Exception e) {
            log.info("工具[bookVisit]预约失败，error={}", e.getMessage());
            return "预约失败，时间格式错误（请使用ISO格式，如2023-10-01T10:00:00）或系统异常: " + e.getMessage();
        }
    }

    @Tool(description = "取消来访预约。输入预约ID。必须基于当前登录家属绑定的老人。")
    public String cancelVisitAppointment(
            @ToolParam(description = "预约ID。") Long appointmentId,
            @ToolParam(description = "老人姓名。如果有多个绑定老人，可通过姓名区分，不填则查默认老人。") String elderName) {
        String resolvedFamilyPhone = resolveFamilyPhone();
        Long resolvedElderId = resolveElderIdForFamily(elderName, resolvedFamilyPhone);
        if (resolvedElderId == null) {
            return "取消失败，" + unresolvedFamilyElderMessage(resolvedFamilyPhone);
        }
        log.info("调用工具[cancelVisitAppointment]，elderId={}，appointmentId={}", resolvedElderId, appointmentId);
        String result = visitAppointmentService.cancelVisitAppointment(resolvedElderId, appointmentId);
        log.info("工具[cancelVisitAppointment]返回结果={}", result);
        return result;
    }

    @Tool(description = "按条件查询来访预约。可按状态和时间范围过滤。必须基于当前登录家属绑定的老人。")
    public String queryVisitAppointments(
            @ToolParam(description = "老人姓名。如果有多个绑定老人，可通过姓名区分，不填则查默认老人。") String elderName,
            @ToolParam(description = "预约状态，可选：PENDING、APPROVED、CANCELED、DONE；不填则不过滤。") String status,
            @ToolParam(description = "开始时间，ISO格式，例如2023-10-01T00:00:00；不填则不限。") String from,
            @ToolParam(description = "结束时间，ISO格式，例如2023-10-31T23:59:59；不填则不限。") String to) {
        try {
            String resolvedFamilyPhone = resolveFamilyPhone();
            Long resolvedElderId = resolveElderIdForFamily(elderName, resolvedFamilyPhone);
            if (resolvedElderId == null) {
                return "查询失败，" + unresolvedFamilyElderMessage(resolvedFamilyPhone);
            }
            LocalDateTime fromTime = elderIdentityHelper.parseDateTimeOrNull(from);
            LocalDateTime toTime = elderIdentityHelper.parseDateTimeOrNull(to);
            String normalizedStatus = elderIdentityHelper.normalizeOrNull(status);
            log.info("调用工具[queryVisitAppointments]，elderId={}，status={}，from={}，to={}", resolvedElderId,
                    normalizedStatus,
                    fromTime, toTime);
            List<VisitAppointment> visits = visitAppointmentService.queryVisitAppointments(resolvedElderId,
                    normalizedStatus,
                    fromTime, toTime);
            if (visits == null || visits.isEmpty()) {
                return "没有符合条件的预约记录。";
            }
            return visits.stream()
                    .map(v -> "预约ID:" + v.getId() + " " + v.getVisitorName() + " (" + v.getRelation() + ") 来访时间: "
                            + v.getVisitTime() + " 状态: " + v.getStatus())
                    .collect(Collectors.joining("; "));
        } catch (DateTimeParseException e) {
            return "查询失败，时间格式错误，请使用ISO格式，例如2023-10-01T10:00:00。";
        }
    }

    @Tool(description = "家属查询老人健康记录。仅支持已登录家属身份查询其绑定的老人。")
    public String queryElderHealth(
            @ToolParam(description = "老人姓名。如果有多个绑定老人，可通过姓名区分，不填则查默认老人。") String elderName,
            @ToolParam(description = "开始日期，格式yyyy-MM-dd；不填默认今天。") String startDate,
            @ToolParam(description = "结束日期，格式yyyy-MM-dd；不填默认与开始日期一致。") String endDate) {
        String resolvedFamilyPhone = resolveFamilyPhone();
        if (resolvedFamilyPhone == null) {
            return "查询失败，请先登录家属账号。";
        }
        Long resolvedElderId = resolveElderIdForFamily(elderName, resolvedFamilyPhone);
        if (resolvedElderId == null) {
            return "查询失败，" + unresolvedFamilyElderMessage(resolvedFamilyPhone);
        }
        try {
            LocalDate start = elderIdentityHelper.parseDateOrNull(startDate);
            LocalDate end = elderIdentityHelper.parseDateOrNull(endDate);
            return healthRecordService.queryElderHealth(resolvedFamilyPhone, resolvedElderId, start, end);
        } catch (DateTimeParseException e) {
            return "查询失败，日期格式错误，请使用yyyy-MM-dd。";
        }
    }

    @Tool(description = "校验当前登录家属是否已绑定某位老人。")
    public String checkFamilyElderAccess(
            @ToolParam(description = "老人姓名。如果有多个绑定老人，可通过姓名区分，不填则查默认老人。") String elderName) {
        String resolvedFamilyPhone = resolveFamilyPhone();
        if (resolvedFamilyPhone == null) {
            return "校验失败，请先登录家属账号。";
        }
        Long resolvedElderId = resolveElderIdForFamily(elderName, resolvedFamilyPhone);
        if (resolvedElderId == null) {
            return "校验失败，" + unresolvedFamilyElderMessage(resolvedFamilyPhone);
        }
        boolean canAccess = familyAccessService.canAccessElder(resolvedFamilyPhone, resolvedElderId);
        if (!canAccess) {
            return "未绑定：当前登录家属与该老人不存在绑定关系。";
        }
        return "已绑定：当前登录家属可访问该老人信息。";
    }

    @Tool(description = "家属查询老人某日概览。仅支持已登录家属身份查询其绑定的老人。")
    public String getElderDailySummary(
            @ToolParam(description = "老人姓名。如果有多个绑定老人，可通过姓名区分，不填则查默认老人。") String elderName,
            @ToolParam(description = "日期，格式yyyy-MM-dd；不填默认今天。") String date) {
        String resolvedFamilyPhone = resolveFamilyPhone();
        if (resolvedFamilyPhone == null) {
            return "查询失败，请先登录家属账号。";
        }
        Long resolvedElderId = resolveElderIdForFamily(elderName, resolvedFamilyPhone);
        if (resolvedElderId == null) {
            return "查询失败，" + unresolvedFamilyElderMessage(resolvedFamilyPhone);
        }
        try {
            LocalDate targetDate = elderIdentityHelper.parseDateOrNull(date);
            log.info("调用工具[getElderDailySummary]，familyPhone={}，elderId={}，date={}", resolvedFamilyPhone,
                    resolvedElderId,
                    targetDate);
            return familyAccessService.getElderDailySummary(resolvedFamilyPhone, resolvedElderId, targetDate);
        } catch (DateTimeParseException e) {
            return "查询失败，日期格式错误，请使用yyyy-MM-dd。";
        }
    }

    /**
     * 解析当前登录家属的手机号
     * 
     * @return 当前登录家属的手机号，如果未登录则返回 null
     */
    private String resolveFamilyPhone() {
        return FamilyLoginContext.get()
                .map(ctx -> ctx.phone())
                .filter(phone -> !phone.isBlank())
                .orElse(null);
    }

    /**
     * 在家属身份范围内解析目标老人ID。
     * 优先按姓名匹配指定老人，未提供或未命中时回退默认绑定老人。
     *
     * @param elderName 老人姓名
     * @param familyPhone 家属手机号
     * @return 解析到的老人ID，未命中返回 null
     */
    private Long resolveElderIdForFamily(String elderName, String familyPhone) {
        if (familyPhone == null || familyPhone.isBlank()) {
            return null;
        }
        if (elderName != null && !elderName.isBlank()) {
            Long id = familyAccessService.resolveElderIdByName(familyPhone, elderName.trim());
            if (id != null) {
                return id;
            }
        }
        return familyAccessService.resolveDefaultElderId(familyPhone);
    }

    /**
     * 构建家属-老人绑定关系未命中时的提示文案。
     *
     * @param familyPhone 家属手机号
     * @return 绑定关系提示信息
     */
    private String unresolvedFamilyElderMessage(String familyPhone) {
        if (familyPhone == null || familyPhone.isBlank()) {
            return "请先登录家属账号。";
        }
        return familyAccessService.buildBoundElderContext(familyPhone);
    }

}
