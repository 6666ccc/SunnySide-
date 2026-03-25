package cn.lc.sunnyside.Service.ServiceImpl;

import cn.lc.sunnyside.POJO.DO.Announcement;
import cn.lc.sunnyside.POJO.DO.ElderlyUser;
import cn.lc.sunnyside.POJO.DO.FamilyElderRelation;
import cn.lc.sunnyside.POJO.DO.FamilyUser;
import cn.lc.sunnyside.POJO.DO.Menu;
import cn.lc.sunnyside.POJO.DO.VisitAppointment;
import cn.lc.sunnyside.POJO.DTO.UserActivityDTO;
import cn.lc.sunnyside.Service.FamilyAccessService;
import cn.lc.sunnyside.mapper.ActivityParticipationMapper;
import cn.lc.sunnyside.mapper.AnnouncementMapper;
import cn.lc.sunnyside.mapper.ElderlyUserMapper;
import cn.lc.sunnyside.mapper.FamilyElderRelationMapper;
import cn.lc.sunnyside.mapper.FamilyUserMapper;
import cn.lc.sunnyside.mapper.MenuMapper;
import cn.lc.sunnyside.mapper.VisitAppointmentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FamilyAccessServiceImpl implements FamilyAccessService {

    private final FamilyUserMapper familyUserMapper;
    private final FamilyElderRelationMapper familyElderRelationMapper;
    private final ActivityParticipationMapper activityParticipationMapper;
    private final MenuMapper menuMapper;
    private final AnnouncementMapper announcementMapper;
    private final VisitAppointmentMapper visitAppointmentMapper;
    private final ElderlyUserMapper elderlyUserMapper;

    /**
     * 验证家属是否有权限访问指定老人的信息
     *
     * @param familyPhone 家属的手机号
     * @param elderId     老人的唯一标识
     * @return 如果存在绑定关系则返回 true，否则返回 false
     */
    @Override
    public boolean canAccessElder(String familyPhone, Long elderId) {
        FamilyUser familyUser = familyUserMapper.selectByPhone(normalizePhone(familyPhone));
        if (familyUser == null || elderId == null) {
            return false;
        }
        Integer count = familyElderRelationMapper.existsRelation(familyUser.getId(), elderId);
        return count != null && count > 0;
    }

    /**
     * 获取老人某日的日常总结信息，包括活动、菜单、公告和探访安排
     *
     * @param familyPhone 家属手机号
     * @param elderId     老人ID
     * @param date        查询的日期（可选，为空则默认当天）
     * @return 格式化后的日常总结文本
     */
    @Override
    public String getElderDailySummary(String familyPhone, Long elderId, LocalDate date) {
        // 首先校验访问权限
        if (!canAccessElder(familyPhone, elderId)) {
            return "查询失败，家属与老人不存在绑定关系。";
        }
        LocalDate targetDate = LocalDate.now();
        if (date != null) {
            targetDate = date;
        }

        // 并行或依次查询老人的各项日程安排
        List<UserActivityDTO> activities = activityParticipationMapper.selectUserActivitiesByElderIdAndDate(elderId,
                targetDate);
        List<String> menuItems = collectMenus(targetDate);
        // 查询重要级别为3（最高级别）且生效的公告
        List<Announcement> announcements = announcementMapper.selectByConditions(3, null, Boolean.TRUE);
        LocalDateTime from = targetDate.atStartOfDay();
        LocalDateTime to = targetDate.plusDays(1).atStartOfDay().minusSeconds(1);
        List<VisitAppointment> visits = visitAppointmentMapper.selectByConditions(elderId, null, from, to);

        // 格式化活动信息
        String activityText = "无活动安排";
        if (activities != null && !activities.isEmpty()) {
            activityText = activities.stream()
                    .map(item -> item.getActivityName() + "(" + item.getStartTime() + "-" + item.getEndTime() + ")")
                    .collect(Collectors.joining("、"));
        }

        // 格式化菜单信息
        String menuText = "无菜单信息";
        if (!menuItems.isEmpty()) {
            menuText = String.join("、", menuItems);
        }

        // 格式化公告信息
        String announcementText = "无公告";
        if (announcements != null && !announcements.isEmpty()) {
            announcementText = announcements.stream().map(Announcement::getTitle).collect(Collectors.joining("、"));
        }

        // 格式化探访信息
        String visitText = "无探访预约";
        if (visits != null && !visits.isEmpty()) {
            visitText = visits.stream().map(v -> v.getVisitorName() + "(" + v.getStatus() + ")")
                    .collect(Collectors.joining("、"));
        }

        // 拼接综合结果
        return "日期:" + targetDate + "\n活动:" + activityText + "\n菜单:" + menuText + "\n公告:" + announcementText + "\n探访:"
                + visitText;
    }

    /**
     * 解析家属的默认老人ID
     *
     * @param familyPhone 家属手机号
     * @return 默认老人的ID。如果有设置主联系人则返回主联系人的老人ID，如果只有一个绑定老人则返回该老人ID，否则返回 null
     */
    @Override
    public Long resolveDefaultElderId(String familyPhone) {
        FamilyUser familyUser = familyUserMapper.selectByPhone(normalizePhone(familyPhone));
        if (familyUser == null) {
            return null;
        }
        List<FamilyElderRelation> relations = familyElderRelationMapper.selectByFamilyId(familyUser.getId());
        if (relations == null || relations.isEmpty()) {
            return null;
        }
        // 筛选被标记为主联系人（默认）的绑定关系
        List<FamilyElderRelation> primaryRelations = relations.stream()
                .filter(item -> Boolean.TRUE.equals(item.getIsPrimaryContact()))
                .toList();
        if (primaryRelations.size() == 1) {
            return primaryRelations.get(0).getElderId();
        }
        // 如果没有主联系人，但只绑定了一个老人，则默认返回该老人
        if (relations.size() == 1) {
            return relations.get(0).getElderId();
        }
        return null;
    }

    /**
     * 根据家属手机号和老人的姓名模糊解析对应的老人ID
     *
     * @param familyPhone 家属手机号
     * @param elderName   老人姓名或称呼
     * @return 匹配的老人ID，如果未找到则返回 null
     */
    @Override
    public Long resolveElderIdByName(String familyPhone, String elderName) {
        FamilyUser familyUser = familyUserMapper.selectByPhone(normalizePhone(familyPhone));
        if (familyUser == null) {
            return null;
        }
        List<FamilyElderRelation> relations = familyElderRelationMapper.selectByFamilyId(familyUser.getId());
        if (relations == null || relations.isEmpty()) {
            return null;
        }
        // 优先尝试精确匹配老人全名
        for (FamilyElderRelation rel : relations) {
            ElderlyUser elder = elderlyUserMapper.selectById(rel.getElderId());
            if (elder != null && elderName.equals(elder.getFullName())) {
                return elder.getId();
            }
        }
        // 如果精确匹配失败，尝试进行包含（模糊）匹配
        for (FamilyElderRelation rel : relations) {
            ElderlyUser elder = elderlyUserMapper.selectById(rel.getElderId());
            if (elder != null && elder.getFullName() != null && elder.getFullName().contains(elderName)) {
                return elder.getId();
            }
        }
        return null;
    }

    /**
     * 构建家属与老人绑定关系的上下文信息（主要用于注入 AI 提示词）
     *
     * @param familyPhone 家属手机号
     * @return 包含该家属所有绑定老人信息及默认老人ID的文本描述
     */
    @Override
    public String buildBoundElderContext(String familyPhone) {
        FamilyUser familyUser = familyUserMapper.selectByPhone(normalizePhone(familyPhone));
        if (familyUser == null) {
            return "未识别到有效家属账号。";
        }
        List<FamilyElderRelation> relations = familyElderRelationMapper.selectByFamilyId(familyUser.getId());
        if (relations == null || relations.isEmpty()) {
            return "当前家属暂无已绑定老人。";
        }

        // 去重，以老人ID为键收集关系映射
        Map<Long, FamilyElderRelation> relationByElder = relations.stream()
                .collect(Collectors.toMap(FamilyElderRelation::getElderId, item -> item, (a, b) -> a,
                        LinkedHashMap::new));

        // 将绑定老人信息排序并格式化为字符串列表
        List<String> elderSummaries = relationByElder.values().stream()
                .sorted(Comparator.comparing(FamilyElderRelation::getIsPrimaryContact,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(FamilyElderRelation::getElderId))
                .map(rel -> {
                    ElderlyUser elder = elderlyUserMapper.selectById(rel.getElderId());
                    if (elder == null) {
                        return "ID:" + rel.getElderId();
                    }
                    String name = "未知";
                    if (elder.getFullName() != null) {
                        name = elder.getFullName();
                    }
                    String defaultSuffix = "";
                    if (Boolean.TRUE.equals(rel.getIsPrimaryContact())) {
                        defaultSuffix = "(默认)";
                    }
                    return "ID:" + elder.getId() + " 姓名:" + name + defaultSuffix;
                })
                .toList();

        // 解析默认老人
        Long defaultElderId = resolveDefaultElderId(familyPhone);
        if (defaultElderId != null) {
            return "已绑定老人:" + String.join("；", elderSummaries) + "。默认老人ID=" + defaultElderId + "，未指定老人时优先使用该对象。";
        }
        return "已绑定老人:" + String.join("；", elderSummaries) + "。当前存在多位绑定老人，请优先让用户提供姓名或手机号后4位。";
    }

    /**
     * 收集指定日期的各类餐食菜单
     *
     * @param date 查询日期
     * @return 按餐食类型格式化后的菜单列表
     */
    private List<String> collectMenus(LocalDate date) {
        List<String> items = new ArrayList<>();
        String[] mealTypes = { "BREAKFAST", "LUNCH", "DINNER", "SNACK" };
        for (String mealType : mealTypes) {
            List<Menu> menus = menuMapper.selectByDateAndType(date, mealType);
            if (menus == null || menus.isEmpty()) {
                continue;
            }
            String text = menus.stream()
                    .map(Menu::getDishName)
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining("/"));
            if (!text.isBlank()) {
                items.add(mealType + ":" + text);
            }
        }
        return items;
    }

    /**
     * 规范化手机号输入
     *
     * @param phone 原始手机号
     * @return 去除两端空格后的手机号，若为空则返回 null
     */
    private String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }
        return phone.trim();
    }
}
