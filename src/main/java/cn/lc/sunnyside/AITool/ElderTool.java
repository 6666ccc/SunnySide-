package cn.lc.sunnyside.AITool;

import cn.lc.sunnyside.POJO.DO.*;
import cn.lc.sunnyside.POJO.DTO.UserActivityDTO;
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
 * 日常类的工具
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ElderTool {

    private final ElderlyUserService elderlyUserService;
    private final MenuService menuService;
    private final ActivityScheduleService activityScheduleService;
    private final MedicalDutyService medicalDutyService;
    private final AnnouncementService announcementService;
    private final ActivityParticipationService activityParticipationService;
    private final VisitAppointmentService visitAppointmentService;
    private final ElderIdentityHelper elderIdentityHelper;

    @Tool(description = "获取系统当前日期时间。适用于用户询问“现在几点”“今天几号”等时间相关问题。返回值为ISO-8601格式的时间字符串。")
    public String getCurrentTime() {
        String now = LocalDateTime.now().toString();
        log.info("调用工具[getCurrentTime]，返回时间={}", now);
        return now;
    }

    @Tool(description = "查询老人当前位置。支持输入老人ID，或输入老人姓名/手机号后4位等身份线索。")
    public String getElderLocation(
            @ToolParam(description = "老人ID。已知时优先传入。") Long elderlyId,
            @ToolParam(description = "老人身份线索，如姓名、手机号后4位等。ID不清楚时使用。") String elderRef) {
        Long resolvedElderId = elderIdentityHelper.resolveElderId(elderlyId, elderRef);
        if (resolvedElderId == null) {
            return elderIdentityHelper.unresolvedMessage(elderRef);
        }
        log.info("调用工具[getElderLocation]，elderId={}", elderlyId);
        String location = elderlyUserService.getElderLocation(resolvedElderId);
        log.info("工具[getElderLocation]返回位置={}", location);
        return location;
    }

    @Tool(description = "查询老人基础信息。支持输入老人ID，或输入老人姓名/手机号后4位等身份线索。")
    public String getElderProfile(
            @ToolParam(description = "老人ID。已知时优先传入。") Long elderlyId,
            @ToolParam(description = "老人身份线索，如姓名、手机号后4位等。ID不清楚时使用。") String elderRef) {
        Long resolvedElderId = elderIdentityHelper.resolveElderId(elderlyId, elderRef);
        if (resolvedElderId == null) {
            return "查询失败，" + elderIdentityHelper.unresolvedMessage(elderRef);
        }
        ElderlyUser elder = elderlyUserService.getById(resolvedElderId);
        if (elder == null) {
            return "查询失败，未找到该老人信息。";
        }
        String phoneTail = "未知";
        if (elder.getPhone() != null && elder.getPhone().length() >= 4) {
            phoneTail = elder.getPhone().substring(elder.getPhone().length() - 4);
        }
        return "老人信息：ID:" + elder.getId()
                + "，姓名:" + elder.getFullName()
                + "，性别:" + (elder.getGender() == null ? "未知" : elder.getGender())
                + "，手机号后4位:" + phoneTail;
    }

    @Tool(description = "查询当前餐次菜单。返回菜品名称及可用营养说明，多个菜品以逗号分隔；无数据时返回“当前没有菜单信息”。")
    public String getCurrentMenu() {
        log.info("调用工具[getCurrentMenu]");
        List<Menu> menus = menuService.getCurrentMenu();
        if (menus == null || menus.isEmpty()) {
            log.info("工具[getCurrentMenu]无菜单数据");
            return "当前没有菜单信息。";
        }
        String menuText = menus.stream()
                .map(menu -> menu.getDishName()
                        + (menu.getNutritionNotes() != null ? " (" + menu.getNutritionNotes() + ")" : ""))
                .collect(Collectors.joining(", "));
        log.info("工具[getCurrentMenu]返回菜单条数={}", menus.size());
        return menuText;
    }

    @Tool(description = "查询今日活动安排。返回活动名称、起止时间、地点和活动ID，多个活动以分号分隔；无活动时返回“今天没有活动安排”。")
    public String getTodayActivities() {
        log.info("调用工具[getTodayActivities]");
        List<ActivitySchedule> activities = activityScheduleService.getTodayActivities();
        if (activities == null || activities.isEmpty()) {
            log.info("工具[getTodayActivities]今日无活动");
            return "今天没有活动安排。";
        }
        String activityText = activities.stream()
                .map(a -> a.getActivityName() + " (" + a.getStartTime() + "-" + a.getEndTime() + ") 地点: "
                        + a.getLocation() + " ID:" + a.getId())
                .collect(Collectors.joining("; "));
        log.info("工具[getTodayActivities]返回活动数量={}", activities.size());
        return activityText;
    }

    @Tool(description = "查询今日值班医护人员。返回值班人员姓名、角色和联系电话，多个记录以分号分隔；无数据时返回“今天没有值班医护记录”。")
    public String getOnDutyStaff() {
        log.info("调用工具[getOnDutyStaff]");
        List<MedicalDuty> staff = medicalDutyService.getOnDutyStaff();
        if (staff == null || staff.isEmpty()) {
            log.info("工具[getOnDutyStaff]今日无值班记录");
            return "今天没有值班医护记录。";
        }
        String staffText = staff.stream()
                .map(s -> s.getStaffName() + " (" + s.getStaffRole() + ") 电话: " + s.getPhone())
                .collect(Collectors.joining("; "));
        log.info("工具[getOnDutyStaff]返回值班人数={}", staff.size());
        return staffText;
    }

    @Tool(description = "查询最近通知公告。默认返回最近5条公告，包含发布日期、标题和内容；无公告时返回“最近没有公告”。")
    public String searchAnnouncements() {
        log.info("调用工具[searchAnnouncements]，limit=5");
        List<Announcement> announcements = announcementService.getRecentAnnouncements(5);
        if (announcements == null || announcements.isEmpty()) {
            log.info("工具[searchAnnouncements]无公告数据");
            return "最近没有公告。";
        }
        String announcementText = announcements.stream()
                .map(a -> "[" + a.getAnnouncementDate() + "] " + a.getTitle() + ": " + a.getContent())
                .collect(Collectors.joining("\n"));
        log.info("工具[searchAnnouncements]返回公告数量={}", announcements.size());
        return announcementText;
    }

    @Tool(description = "活动报名工具。输入活动ID，并提供老人ID或老人身份线索后提交报名请求。")
    public String registerActivity(
            @ToolParam(description = "活动ID。") Long activityId,
            @ToolParam(description = "老人ID。已知时优先传入。") Long elderlyId,
            @ToolParam(description = "老人身份线索，如姓名、手机号后4位等。ID不清楚时使用。") String elderRef) {
        Long resolvedElderId = elderIdentityHelper.resolveElderId(elderlyId, elderRef);
        if (resolvedElderId == null) {
            return "报名失败，" + elderIdentityHelper.unresolvedMessage(elderRef);
        }
        log.info("调用工具[registerActivity]，elderId={}，activityId={}", resolvedElderId, activityId);
        String result = activityParticipationService.registerActivity(resolvedElderId, activityId);
        log.info("工具[registerActivity]返回结果={}", result);
        return result;
    }

    @Tool(description = "取消活动报名。输入活动ID，并提供老人ID或老人身份线索后取消报名。")
    public String cancelActivityRegistration(
            @ToolParam(description = "活动ID。") Long activityId,
            @ToolParam(description = "老人ID。已知时优先传入。") Long elderlyId,
            @ToolParam(description = "老人身份线索，如姓名、手机号后4位等。ID不清楚时使用。") String elderRef) {
        Long resolvedElderId = elderIdentityHelper.resolveElderId(elderlyId, elderRef);
        if (resolvedElderId == null) {
            return "取消失败，" + elderIdentityHelper.unresolvedMessage(elderRef);
        }
        log.info("调用工具[cancelActivityRegistration]，elderId={}，activityId={}", resolvedElderId, activityId);
        String result = activityParticipationService.cancelActivityRegistration(resolvedElderId, activityId);
        log.info("工具[cancelActivityRegistration]返回结果={}", result);
        return result;
    }

    @Tool(description = "查询某位老人某天已报名活动。日期格式为yyyy-MM-dd，支持老人ID或身份线索。")
    public String getMyActivityRegistrations(
            @ToolParam(description = "查询日期，格式yyyy-MM-dd。") String date,
            @ToolParam(description = "老人ID。已知时优先传入。") Long elderlyId,
            @ToolParam(description = "老人身份线索，如姓名、手机号后4位等。ID不清楚时使用。") String elderRef) {
        try {
            LocalDate targetDate = LocalDate.parse(date);
            Long resolvedElderId = elderIdentityHelper.resolveElderId(elderlyId, elderRef);
            if (resolvedElderId == null) {
                return "查询失败，" + elderIdentityHelper.unresolvedMessage(elderRef);
            }
            log.info("调用工具[getMyActivityRegistrations]，elderId={}，date={}", resolvedElderId, targetDate);
            List<UserActivityDTO> activities = activityParticipationService.getMyActivityRegistrations(resolvedElderId,
                    targetDate);
            if (activities == null || activities.isEmpty()) {
                return "该日期没有活动报名记录。";
            }
            return activities.stream()
                    .map(a -> a.getActivityName() + " (" + a.getStartTime() + "-" + a.getEndTime() + ") 地点: "
                            + a.getLocation() + " 状态: " + a.getParticipationStatus() + " ID:" + a.getActivityId())
                    .collect(Collectors.joining("; "));
        } catch (DateTimeParseException e) {
            return "查询失败，日期格式错误，请使用yyyy-MM-dd。";
        }
    }

    @Tool(description = "查询某位老人近期来访预约。支持老人ID或身份线索。")
    public String getVisitors(
            @ToolParam(description = "老人ID。已知时优先传入。") Long elderlyId,
            @ToolParam(description = "老人身份线索，如姓名、手机号后4位等。ID不清楚时使用。") String elderRef) {
        Long resolvedElderId = elderIdentityHelper.resolveElderId(elderlyId, elderRef);
        if (resolvedElderId == null) {
            return "查询失败，" + elderIdentityHelper.unresolvedMessage(elderRef);
        }
        log.info("调用工具[getVisitors]，elderId={}", resolvedElderId);
        List<VisitAppointment> visits = visitAppointmentService.getVisitors(resolvedElderId);
        if (visits == null || visits.isEmpty()) {
            log.info("工具[getVisitors]无来访预约");
            return "近期没有人预约看望。";
        }
        String visitorText = visits.stream()
                .map(v -> v.getVisitorName() + " (" + v.getRelation() + ") 将于 " + v.getVisitTime() + " 来访")
                .collect(Collectors.joining("; "));
        log.info("工具[getVisitors]返回预约数量={}", visits.size());
        return visitorText;
    }

    @Tool(description = "按条件查询某位老人的来访预约。支持按状态和时间范围过滤，便于后续取消预约。")
    public String queryMyVisitAppointments(
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
            List<VisitAppointment> visits = visitAppointmentService.queryVisitAppointments(
                    resolvedElderId, normalizedStatus, fromTime, toTime);
            if (visits == null || visits.isEmpty()) {
                return "没有符合条件的来访预约。";
            }
            return visits.stream()
                    .map(v -> "预约ID:" + v.getId() + " " + v.getVisitorName() + " (" + v.getRelation() + ") 来访时间: "
                            + v.getVisitTime() + " 状态: " + v.getStatus())
                    .collect(Collectors.joining("; "));
        } catch (DateTimeParseException e) {
            return "查询失败，时间格式错误，请使用ISO格式，例如2023-10-01T10:00:00。";
        }
    }

    @Tool(description = "按日期和餐次查询菜单。日期格式yyyy-MM-dd，餐次可选BREAKFAST、LUNCH、DINNER、SNACK。")
    public String getMenuByDateAndMeal(
            @ToolParam(description = "菜单日期，格式yyyy-MM-dd。") String date,
            @ToolParam(description = "餐次类型：BREAKFAST、LUNCH、DINNER、SNACK。") String mealType) {
        try {
            LocalDate targetDate = LocalDate.parse(date);
            String normalizedMealType = elderIdentityHelper.normalizeRequired(mealType);
            log.info("调用工具[getMenuByDateAndMeal]，date={}，mealType={}", targetDate, normalizedMealType);
            List<Menu> menus = menuService.getMenuByDateAndMeal(targetDate, normalizedMealType);
            if (menus == null || menus.isEmpty()) {
                return "该日期该餐次没有菜单信息。";
            }
            return menus.stream()
                    .map(menu -> menu.getDishName()
                            + (menu.getNutritionNotes() != null ? " (" + menu.getNutritionNotes() + ")" : ""))
                    .collect(Collectors.joining(", "));
        } catch (DateTimeParseException e) {
            return "查询失败，日期格式错误，请使用yyyy-MM-dd。";
        }
    }

    @Tool(description = "按条数、优先级和是否有效查询公告。优先级可选LOW、MEDIUM、HIGH。")
    public String getAnnouncements(
            @ToolParam(description = "返回条数，例如5。") Integer limit,
            @ToolParam(description = "优先级，可选LOW、MEDIUM、HIGH；不填则不过滤。") String priority,
            @ToolParam(description = "是否只看有效公告。true为只看有效，false为不过滤。") Boolean activeOnly) {
        int normalizedLimit = (limit == null || limit <= 0) ? 5 : limit;
        String normalizedPriority = elderIdentityHelper.normalizeOrNull(priority);
        Boolean normalizedActiveOnly = activeOnly == null ? Boolean.TRUE : activeOnly;
        log.info("调用工具[getAnnouncements]，limit={}，priority={}，activeOnly={}", normalizedLimit, normalizedPriority,
                normalizedActiveOnly);
        List<Announcement> announcements = announcementService.getAnnouncements(normalizedLimit, normalizedPriority,
                normalizedActiveOnly);
        if (announcements == null || announcements.isEmpty()) {
            return "没有符合条件的公告。";
        }
        return announcements.stream()
                .map(a -> "[" + a.getAnnouncementDate() + "][" + a.getPriority() + "] " + a.getTitle() + ": "
                        + a.getContent())
                .collect(Collectors.joining("\n"));
    }

    @Tool(description = "获取某位老人的健康提醒信息。支持老人ID或身份线索。")
    public String getHealthReminder(
            @ToolParam(description = "老人ID。已知时优先传入。") Long elderlyId,
            @ToolParam(description = "老人身份线索，如姓名、手机号后4位等。ID不清楚时使用。") String elderRef) {
        Long resolvedElderId = elderIdentityHelper.resolveElderId(elderlyId, elderRef);
        if (resolvedElderId == null) {
            return "查询失败，" + elderIdentityHelper.unresolvedMessage(elderRef);
        }
        log.info("调用工具[getHealthReminder]，elderId={}", resolvedElderId);
        String reminder = elderlyUserService.getHealthReminder(resolvedElderId);
        log.info("工具[getHealthReminder]返回提醒={}", reminder);
        return reminder;
    }

    @Tool(description = "解析老人身份线索并返回匹配结果。输入姓名、手机号后4位或老人ID。若唯一命中会明确给出老人ID；若重名会返回候选列表。")
    public String resolveElderIdentity(@ToolParam(description = "老人身份线索，例如姓名、手机号后4位或ID。") String elderRef) {
        List<ElderlyUser> candidates = elderIdentityHelper.findCandidates(elderRef);
        if (candidates.isEmpty()) {
            return "未找到匹配老人，请补充姓名全称或手机号后4位。";
        }
        if (candidates.size() == 1) {
            ElderlyUser matched = candidates.get(0);
            return "已匹配老人: " + elderIdentityHelper.formatElderBrief(matched) + "。后续调用可直接使用老人ID " + matched.getId()
                    + "。";
        }
        return "匹配到多位老人，请先确认具体对象: " + candidates.stream()
                .map(elderIdentityHelper::formatElderBrief)
                .collect(Collectors.joining("; "));
    }

}
