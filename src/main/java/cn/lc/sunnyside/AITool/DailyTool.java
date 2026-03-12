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
public class DailyTool {

    private final ElderlyUserService elderlyUserService;
    private final MenuService menuService;
    private final ActivityScheduleService activityScheduleService;
    private final MedicalDutyService medicalDutyService;
    private final AnnouncementService announcementService;
    private final ActivityParticipationService activityParticipationService;
    private final VisitAppointmentService visitAppointmentService;

    // 获取当前时间
    @Tool(description = "获取系统当前日期时间。适用于用户询问“现在几点”“今天几号”等时间相关问题。返回值为ISO-8601格式的时间字符串。")
    public String getCurrentTime() {
        String now = LocalDateTime.now().toString();
        log.info("调用工具[getCurrentTime]，返回时间={}", now);
        return now;
    }


    @Tool(description = "查询老人当前位置。输入老人姓名、称呼或老人ID。")
    public String getElderLocation(@ToolParam(description = "老人姓氏或包含老人称呼的文本，例如“李”“老刘”“李华在哪”。") String surnameOrText) {
        Long elderlyId = resolveElderId(surnameOrText);
        if (elderlyId == null) {
            return "未识别到有效老人信息，请提供老人姓名或老人ID。";
        }
        log.info("调用工具[getElderLocation]，elderId={}", elderlyId);
        String location = elderlyUserService.getElderLocation(elderlyId);
        log.info("工具[getElderLocation]返回位置={}", location);
        return location;
    }

    private Long resolveElderId(String elderInfo) {
        if (elderInfo == null || elderInfo.isBlank()) {
            return null;
        }
        String normalized = elderInfo.trim();
        if (normalized.matches("\\d+")) {
            return Long.parseLong(normalized);
        }
        String surname = extractSurname(normalized);
        if (surname.isBlank()) {
            return null;
        }
        ElderlyUser elderlyUser = elderlyUserService.findElderlyUserBySurname(surname);
        if (elderlyUser == null) {
            return null;
        }
        return elderlyUser.getId();
    }

    private String extractSurname(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String normalized = text.trim();
        if (normalized.length() >= 2 && normalized.charAt(0) == '老' && isChinese(normalized.charAt(1))) {
            return String.valueOf(normalized.charAt(1));
        }
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if (isChinese(c)) {
                return String.valueOf(c);
            }
        }

        return normalized.substring(0, 1);
    }

    private boolean isChinese(char c) {
        return c >= '\u4e00' && c <= '\u9fa5';
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
                .map(menu -> menu.getDishName() + (menu.getNutritionNotes() != null ? " (" + menu.getNutritionNotes() + ")" : ""))
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
                .map(a -> a.getActivityName() + " (" + a.getStartTime() + "-" + a.getEndTime() + ") 地点: " + a.getLocation() + " ID:" + a.getId())
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

    @Tool(description = "活动报名工具。输入老人信息与活动ID后提交报名请求。")
    public String registerActivity(
            @ToolParam(description = "活动ID。") Long activityId,
            @ToolParam(description = "老人姓名、称呼或老人ID。") String elderInfo) {
        Long elderlyId = resolveElderId(elderInfo);
        if (elderlyId == null) {
            return "报名失败，未识别到有效老人信息。";
        }
        log.info("调用工具[registerActivity]，elderId={}，activityId={}", elderlyId, activityId);
        String result = activityParticipationService.registerActivity(elderlyId, activityId);
        log.info("工具[registerActivity]返回结果={}", result);
        return result;
    }

    @Tool(description = "取消活动报名。输入老人信息与活动ID后取消报名。")
    public String cancelActivityRegistration(
            @ToolParam(description = "活动ID。") Long activityId,
            @ToolParam(description = "老人姓名、称呼或老人ID。") String elderInfo) {
        Long elderlyId = resolveElderId(elderInfo);
        if (elderlyId == null) {
            return "取消失败，未识别到有效老人信息。";
        }
        log.info("调用工具[cancelActivityRegistration]，elderId={}，activityId={}", elderlyId, activityId);
        String result = activityParticipationService.cancelActivityRegistration(elderlyId, activityId);
        log.info("工具[cancelActivityRegistration]返回结果={}", result);
        return result;
    }

    @Tool(description = "查询某位老人某天已报名活动。日期格式为yyyy-MM-dd。")
    public String getMyActivityRegistrations(
            @ToolParam(description = "查询日期，格式yyyy-MM-dd。") String date,
            @ToolParam(description = "老人姓名、称呼或老人ID。") String elderInfo) {
        try {
            LocalDate targetDate = LocalDate.parse(date);
            Long elderlyId = resolveElderId(elderInfo);
            if (elderlyId == null) {
                return "查询失败，未识别到有效老人信息。";
            }
            log.info("调用工具[getMyActivityRegistrations]，elderId={}，date={}", elderlyId, targetDate);
            List<UserActivityDTO> activities = activityParticipationService.getMyActivityRegistrations(elderlyId, targetDate);
            if (activities == null || activities.isEmpty()) {
                return "该日期没有活动报名记录。";
            }
            return activities.stream()
                    .map(a -> a.getActivityName() + " (" + a.getStartTime() + "-" + a.getEndTime() + ") 地点: " + a.getLocation() + " 状态: " + a.getParticipationStatus() + " ID:" + a.getActivityId())
                    .collect(Collectors.joining("; "));
        } catch (DateTimeParseException e) {
            return "查询失败，日期格式错误，请使用yyyy-MM-dd。";
        }
    }

    @Tool(description = "预约看望工具。输入老人信息、访客姓名、来访时间、关系和联系电话。")
    public String bookVisit(
            @ToolParam(description = "老人姓名、称呼或老人ID。") String elderInfo,
            @ToolParam(description = "访客姓名。") String visitorName,
            @ToolParam(description = "来访时间（ISO格式，例如2023-10-01T10:00:00）。") String time,
            @ToolParam(description = "关系。") String relation,
            @ToolParam(description = "联系电话。") String phone) {
        Long elderlyId = resolveElderId(elderInfo);
        if (elderlyId == null) {
            return "预约失败，未识别到有效老人信息。";
        }
        log.info("调用工具[bookVisit]，elderId={}，visitorName={}，time={}，relation={}，phone={}",
                elderlyId, visitorName, time, relation, phone);
        try {
            LocalDateTime visitTime = LocalDateTime.parse(time);
            String result = visitAppointmentService.bookVisit(elderlyId, visitorName, phone, visitTime, relation);
            log.info("工具[bookVisit]预约结果={}", result);
            return result;
        } catch (Exception e) {
            log.info("工具[bookVisit]预约失败，error={}", e.getMessage());
            return "预约失败，时间格式错误（请使用ISO格式，如2023-10-01T10:00:00）或系统异常: " + e.getMessage();
        }
    }

    @Tool(description = "取消来访预约。输入老人信息与预约ID后取消预约。")
    public String cancelVisitAppointment(
            @ToolParam(description = "预约ID。") Long appointmentId,
            @ToolParam(description = "老人姓名、称呼或老人ID。") String elderInfo) {
        Long elderlyId = resolveElderId(elderInfo);
        if (elderlyId == null) {
            return "取消失败，未识别到有效老人信息。";
        }
        log.info("调用工具[cancelVisitAppointment]，elderId={}，appointmentId={}", elderlyId, appointmentId);
        String result = visitAppointmentService.cancelVisitAppointment(elderlyId, appointmentId);
        log.info("工具[cancelVisitAppointment]返回结果={}", result);
        return result;
    }

    @Tool(description = "按条件查询来访预约。可按状态和时间范围过滤，时间使用ISO格式。")
    public String queryVisitAppointments(
            @ToolParam(description = "老人姓名、称呼或老人ID。") String elderInfo,
            @ToolParam(description = "预约状态，可选：PENDING、APPROVED、CANCELED、DONE；不填则不过滤。") String status,
            @ToolParam(description = "开始时间，ISO格式，例如2023-10-01T00:00:00；不填则不限。") String from,
            @ToolParam(description = "结束时间，ISO格式，例如2023-10-31T23:59:59；不填则不限。") String to) {
        try {
            Long elderlyId = resolveElderId(elderInfo);
            if (elderlyId == null) {
                return "查询失败，未识别到有效老人信息。";
            }
            LocalDateTime fromTime = parseDateTimeOrNull(from);
            LocalDateTime toTime = parseDateTimeOrNull(to);
            String normalizedStatus = normalizeOrNull(status);
            log.info("调用工具[queryVisitAppointments]，elderId={}，status={}，from={}，to={}", elderlyId, normalizedStatus, fromTime, toTime);
            List<VisitAppointment> visits = visitAppointmentService.queryVisitAppointments(elderlyId, normalizedStatus, fromTime, toTime);
            if (visits == null || visits.isEmpty()) {
                return "没有符合条件的预约记录。";
            }
            return visits.stream()
                    .map(v -> "预约ID:" + v.getId() + " " + v.getVisitorName() + " (" + v.getRelation() + ") 来访时间: " + v.getVisitTime() + " 状态: " + v.getStatus())
                    .collect(Collectors.joining("; "));
        } catch (DateTimeParseException e) {
            return "查询失败，时间格式错误，请使用ISO格式，例如2023-10-01T10:00:00。";
        }
    }

    @Tool(description = "查询某位老人近期来访预约。")
    public String getVisitors(@ToolParam(description = "老人姓名、称呼或老人ID。") String elderInfo) {
        Long elderlyId = resolveElderId(elderInfo);
        if (elderlyId == null) {
            return "查询失败，未识别到有效老人信息。";
        }
        log.info("调用工具[getVisitors]，elderId={}", elderlyId);
        List<VisitAppointment> visits = visitAppointmentService.getVisitors(elderlyId);
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

    @Tool(description = "按日期和餐次查询菜单。日期格式yyyy-MM-dd，餐次可选BREAKFAST、LUNCH、DINNER、SNACK。")
    public String getMenuByDateAndMeal(
            @ToolParam(description = "菜单日期，格式yyyy-MM-dd。") String date,
            @ToolParam(description = "餐次类型：BREAKFAST、LUNCH、DINNER、SNACK。") String mealType) {
        try {
            LocalDate targetDate = LocalDate.parse(date);
            String normalizedMealType = normalizeRequired(mealType);
            log.info("调用工具[getMenuByDateAndMeal]，date={}，mealType={}", targetDate, normalizedMealType);
            List<Menu> menus = menuService.getMenuByDateAndMeal(targetDate, normalizedMealType);
            if (menus == null || menus.isEmpty()) {
                return "该日期该餐次没有菜单信息。";
            }
            return menus.stream()
                    .map(menu -> menu.getDishName() + (menu.getNutritionNotes() != null ? " (" + menu.getNutritionNotes() + ")" : ""))
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
        String normalizedPriority = normalizeOrNull(priority);
        Boolean normalizedActiveOnly = activeOnly == null ? Boolean.TRUE : activeOnly;
        log.info("调用工具[getAnnouncements]，limit={}，priority={}，activeOnly={}", normalizedLimit, normalizedPriority, normalizedActiveOnly);
        List<Announcement> announcements = announcementService.getAnnouncements(normalizedLimit, normalizedPriority, normalizedActiveOnly);
        if (announcements == null || announcements.isEmpty()) {
            return "没有符合条件的公告。";
        }
        return announcements.stream()
                .map(a -> "[" + a.getAnnouncementDate() + "][" + a.getPriority() + "] " + a.getTitle() + ": " + a.getContent())
                .collect(Collectors.joining("\n"));
    }

    @Tool(description = "获取某位老人的健康提醒信息。")
    public String getHealthReminder(@ToolParam(description = "老人姓名、称呼或老人ID。") String elderInfo) {
        Long elderlyId = resolveElderId(elderInfo);
        if (elderlyId == null) {
            return "查询失败，未识别到有效老人信息。";
        }
        log.info("调用工具[getHealthReminder]，elderId={}", elderlyId);
        String reminder = elderlyUserService.getHealthReminder(elderlyId);
        log.info("工具[getHealthReminder]返回提醒={}", reminder);
        return reminder;
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

    private String normalizeRequired(String text) {
        if (text == null) {
            return "";
        }
        return text.trim().toUpperCase();
    }

}
