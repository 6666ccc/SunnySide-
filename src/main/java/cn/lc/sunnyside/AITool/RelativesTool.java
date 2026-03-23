package cn.lc.sunnyside.AITool;

import cn.lc.sunnyside.Auth.FamilyLoginContextHolder;
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

    @Tool(description = "预约看望工具。输入访客信息，并提供老人ID或老人身份线索。")
    public String bookVisit(
            @ToolParam(description = "老人ID。已知时优先传入。") Long elderlyId,
            @ToolParam(description = "老人身份线索，如姓名、手机号后4位等。ID不清楚时使用。") String elderRef,
            @ToolParam(description = "访客姓名。") String visitorName,
            @ToolParam(description = "来访时间（ISO格式，例如2023-10-01T10:00:00）。") String time,
            @ToolParam(description = "关系。") String relation,
            @ToolParam(description = "联系电话。") String phone) {
        Long resolvedElderId = elderIdentityHelper.resolveElderId(elderlyId, elderRef);
        if (resolvedElderId == null) {
            return "预约失败，" + elderIdentityHelper.unresolvedMessage(elderRef);
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

    @Tool(description = "取消来访预约。输入预约ID，并提供老人ID或老人身份线索。")
    public String cancelVisitAppointment(
            @ToolParam(description = "预约ID。") Long appointmentId,
            @ToolParam(description = "老人ID。已知时优先传入。") Long elderlyId,
            @ToolParam(description = "老人身份线索，如姓名、手机号后4位等。ID不清楚时使用。") String elderRef) {
        Long resolvedElderId = elderIdentityHelper.resolveElderId(elderlyId, elderRef);
        if (resolvedElderId == null) {
            return "取消失败，" + elderIdentityHelper.unresolvedMessage(elderRef);
        }
        log.info("调用工具[cancelVisitAppointment]，elderId={}，appointmentId={}", resolvedElderId, appointmentId);
        String result = visitAppointmentService.cancelVisitAppointment(resolvedElderId, appointmentId);
        log.info("工具[cancelVisitAppointment]返回结果={}", result);
        return result;
    }

    @Tool(description = "按条件查询来访预约。支持老人ID或身份线索，可按状态和时间范围过滤。")
    public String queryVisitAppointments(
            @ToolParam(description = "老人ID。已知时优先传入。") Long elderlyId,
            @ToolParam(description = "老人身份线索，如姓名、手机号后4位等。ID不清楚时使用。") String elderRef,
            @ToolParam(description = "预约状态，可选：PENDING、APPROVED、CANCELED、DONE；不填则不过滤。") String status,
            @ToolParam(description = "开始时间，ISO格式，例如2023-10-01T00:00:00；不填则不限。") String from,
            @ToolParam(description = "结束时间，ISO格式，例如2023-10-31T23:59:59；不填则不限。") String to) {
        try {
            Long resolvedElderId = elderIdentityHelper.resolveElderId(elderlyId, elderRef);
            if (resolvedElderId == null) {
                return "查询失败，" + elderIdentityHelper.unresolvedMessage(elderRef);
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

    @Tool(description = "家属查询老人健康记录。仅支持已登录家属身份，并提供老人ID或身份线索。")
    public String queryElderHealth(
            @ToolParam(description = "家属手机号参数已废弃，将自动使用当前登录家属身份。") String familyPhone,
            @ToolParam(description = "老人ID。已知时优先传入。") Long elderlyId,
            @ToolParam(description = "老人身份线索，如姓名、手机号后4位等。ID不清楚时使用。") String elderRef,
            @ToolParam(description = "开始日期，格式yyyy-MM-dd；不填默认今天。") String startDate,
            @ToolParam(description = "结束日期，格式yyyy-MM-dd；不填默认与开始日期一致。") String endDate) {
        // 尝试解析老人ID，如果无法解析则返回错误信息
        Long resolvedElderId = elderIdentityHelper.resolveElderId(elderlyId, elderRef);
        if (resolvedElderId == null) {
            return "查询失败，" + elderIdentityHelper.unresolvedMessage(elderRef);
        }
        String resolvedFamilyPhone = resolveFamilyPhone(familyPhone);
        if (resolvedFamilyPhone == null) {
            return "查询失败，请先登录家属账号。";
        }
        try {

            LocalDate start = elderIdentityHelper.parseDateOrNull(startDate);
            LocalDate end = elderIdentityHelper.parseDateOrNull(endDate);
            // 调用服务查询健康记录，并返回结果
            return healthRecordService.queryElderHealth(resolvedFamilyPhone, resolvedElderId, start, end);
        } catch (DateTimeParseException e) {
            return "查询失败，日期格式错误，请使用yyyy-MM-dd。";
        }
    }

    @Tool(description = "校验当前登录家属是否已绑定某位老人。支持老人ID或身份线索。")
    public String checkFamilyElderAccess(
            @ToolParam(description = "家属手机号参数已废弃，将自动使用当前登录家属身份。") String familyPhone,
            @ToolParam(description = "老人ID。已知时优先传入。") Long elderlyId,
            @ToolParam(description = "老人身份线索，如姓名、手机号后4位等。ID不清楚时使用。") String elderRef) {
        Long resolvedElderId = elderIdentityHelper.resolveElderId(elderlyId, elderRef);
        if (resolvedElderId == null) {
            return "校验失败，" + elderIdentityHelper.unresolvedMessage(elderRef);
        }
        String resolvedFamilyPhone = resolveFamilyPhone(familyPhone);
        if (resolvedFamilyPhone == null) {
            return "校验失败，请先登录家属账号。";
        }
        boolean canAccess = familyAccessService.canAccessElder(resolvedFamilyPhone, resolvedElderId);
        if (!canAccess) {
            return "未绑定：当前登录家属与该老人不存在绑定关系。";
        }
        return "已绑定：当前登录家属可访问该老人信息。";
    }

    @Tool(description = "家属查询老人某日概览。仅支持已登录家属身份，并提供日期与老人信息。")
    public String getElderDailySummary(
            @ToolParam(description = "家属手机号参数已废弃，将自动使用当前登录家属身份。") String familyPhone,
            @ToolParam(description = "老人ID。已知时优先传入。") Long elderlyId,
            @ToolParam(description = "老人身份线索，如姓名、手机号后4位等。ID不清楚时使用。") String elderRef,
            @ToolParam(description = "日期，格式yyyy-MM-dd；不填默认今天。") String date) {
        Long resolvedElderId = elderIdentityHelper.resolveElderId(elderlyId, elderRef);
        if (resolvedElderId == null) {
            return "查询失败，" + elderIdentityHelper.unresolvedMessage(elderRef);
        }
        String resolvedFamilyPhone = resolveFamilyPhone(familyPhone);
        if (resolvedFamilyPhone == null) {
            return "查询失败，请先登录家属账号。";
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
     * @param familyPhone 传入的家属手机号参数（已废弃）
     * @return 当前登录家属的手机号，如果未登录则返回 null
     */
    private String resolveFamilyPhone(String familyPhone) {
        return FamilyLoginContextHolder.get()
                .map(ctx -> ctx.phone())
                .filter(phone -> !phone.isBlank())
                .orElse(null);
    }

}
