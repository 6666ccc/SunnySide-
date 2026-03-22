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
import java.util.Comparator;
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
    private final ElderlyUserService elderlyUserService;

    @Tool(description = "预约看望工具。输入访客信息，并提供老人ID或老人身份线索。")
    public String bookVisit(
            @ToolParam(description = "老人ID。已知时优先传入。") Long elderlyId,
            @ToolParam(description = "老人身份线索，如姓名、手机号后4位等。ID不清楚时使用。") String elderRef,
            @ToolParam(description = "访客姓名。") String visitorName,
            @ToolParam(description = "来访时间（ISO格式，例如2023-10-01T10:00:00）。") String time,
            @ToolParam(description = "关系。") String relation,
            @ToolParam(description = "联系电话。") String phone) {
        Long resolvedElderId = resolveElderId(elderlyId, elderRef);// 尝试解析老人ID
        if (resolvedElderId == null) {
            return "预约失败，" + unresolvedMessage(elderRef);
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
        Long resolvedElderId = resolveElderId(elderlyId, elderRef);
        if (resolvedElderId == null) {
            return "取消失败，" + unresolvedMessage(elderRef);
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
            Long resolvedElderId = resolveElderId(elderlyId, elderRef);
            if (resolvedElderId == null) {
                return "查询失败，" + unresolvedMessage(elderRef);
            }
            LocalDateTime fromTime = parseDateTimeOrNull(from);
            LocalDateTime toTime = parseDateTimeOrNull(to);
            String normalizedStatus = normalizeOrNull(status);
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

    @Tool(description = "家属查询老人健康记录。优先使用已登录家属身份，也支持显式输入家属手机号；并提供老人ID或身份线索。")
    public String queryElderHealth(
            @ToolParam(description = "家属手机号。已登录时可不填。") String familyPhone,
            @ToolParam(description = "老人ID。已知时优先传入。") Long elderlyId,
            @ToolParam(description = "老人身份线索，如姓名、手机号后4位等。ID不清楚时使用。") String elderRef,
            @ToolParam(description = "开始日期，格式yyyy-MM-dd；不填默认今天。") String startDate,
            @ToolParam(description = "结束日期，格式yyyy-MM-dd；不填默认与开始日期一致。") String endDate) {
        Long resolvedElderId = resolveElderId(elderlyId, elderRef);
        if (resolvedElderId == null) {
            return "查询失败，" + unresolvedMessage(elderRef);
        }
        String resolvedFamilyPhone = resolveFamilyPhone(familyPhone);
        if (resolvedFamilyPhone == null) {
            return "查询失败，请先登录家属账号，或提供家属手机号。";
        }
        try {
            LocalDate start = parseDateOrNull(startDate);
            LocalDate end = parseDateOrNull(endDate);
            log.info("调用工具[queryElderHealth]，familyPhone={}，elderId={}，start={}，end={}", resolvedFamilyPhone,
                    resolvedElderId,
                    start,
                    end);
            return healthRecordService.queryElderHealth(resolvedFamilyPhone, resolvedElderId, start, end);
        } catch (DateTimeParseException e) {
            return "查询失败，日期格式错误，请使用yyyy-MM-dd。";
        }
    }

    @Tool(description = "家属查询老人某日概览。优先使用已登录家属身份，也支持显式输入家属手机号；并提供日期与老人信息。")
    public String getElderDailySummary(
            @ToolParam(description = "家属手机号。已登录时可不填。") String familyPhone,
            @ToolParam(description = "老人ID。已知时优先传入。") Long elderlyId,
            @ToolParam(description = "老人身份线索，如姓名、手机号后4位等。ID不清楚时使用。") String elderRef,
            @ToolParam(description = "日期，格式yyyy-MM-dd；不填默认今天。") String date) {
        Long resolvedElderId = resolveElderId(elderlyId, elderRef);
        if (resolvedElderId == null) {
            return "查询失败，" + unresolvedMessage(elderRef);
        }
        String resolvedFamilyPhone = resolveFamilyPhone(familyPhone);
        if (resolvedFamilyPhone == null) {
            return "查询失败，请先登录家属账号，或提供家属手机号。";
        }
        try {
            LocalDate targetDate = parseDateOrNull(date);
            log.info("调用工具[getElderDailySummary]，familyPhone={}，elderId={}，date={}", resolvedFamilyPhone,
                    resolvedElderId,
                    targetDate);
            return familyAccessService.getElderDailySummary(resolvedFamilyPhone, resolvedElderId, targetDate);
        } catch (DateTimeParseException e) {
            return "查询失败，日期格式错误，请使用yyyy-MM-dd。";
        }
    }

    private LocalDate parseDateOrNull(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return LocalDate.parse(text.trim());
    }

    private LocalDateTime parseDateTimeOrNull(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return LocalDateTime.parse(text.trim());
    }

    private String normalizeOrNull(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return text.trim().toUpperCase();
    }

    private String resolveFamilyPhone(String familyPhone) {
        if (familyPhone != null && !familyPhone.isBlank()) {
            return familyPhone.trim();
        }
        return FamilyLoginContextHolder.get()
                .map(ctx -> ctx.phone())
                .filter(phone -> !phone.isBlank())
                .orElse(null);
    }

    private Long resolveElderId(Long elderlyId, String elderRef) {
        if (elderlyId != null) {
            return elderlyId;
        }
        List<ElderlyUser> candidates = findCandidates(elderRef);
        if (candidates.size() == 1) {
            return candidates.get(0).getId();
        }
        return null;
    }

    private List<ElderlyUser> findCandidates(String elderRef) {
        if (elderRef == null || elderRef.isBlank()) {
            return List.of();
        }
        return elderlyUserService.findByRef(elderRef).stream()
                .sorted(Comparator.comparing(ElderlyUser::getId))
                .toList();
    }

    private String unresolvedMessage(String elderRef) {
        List<ElderlyUser> candidates = findCandidates(elderRef);
        if (candidates.isEmpty()) {
            return "未找到匹配老人，请提供老人姓名或手机号后4位。";
        }
        return "匹配到多位老人，请先确认具体对象: " + candidates.stream()
                .map(this::formatElderBrief)
                .collect(Collectors.joining("; "));
    }

    private String formatElderBrief(ElderlyUser elder) {
        String phoneTail = "";
        if (elder.getPhone() != null && elder.getPhone().length() >= 4) {
            phoneTail = " 手机尾号:" + elder.getPhone().substring(elder.getPhone().length() - 4);
        }
        return "ID:" + elder.getId() + " 姓名:" + elder.getFullName() + phoneTail;
    }

}
