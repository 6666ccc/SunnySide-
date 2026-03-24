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

    @Override
    public boolean canAccessElder(String familyPhone, Long elderId) {
        FamilyUser familyUser = familyUserMapper.selectByPhone(normalizePhone(familyPhone));
        if (familyUser == null || elderId == null) {
            return false;
        }
        Integer count = familyElderRelationMapper.existsRelation(familyUser.getId(), elderId);
        return count != null && count > 0;
    }

    @Override
    public String getElderDailySummary(String familyPhone, Long elderId, LocalDate date) {
        if (!canAccessElder(familyPhone, elderId)) {
            return "查询失败，家属与老人不存在绑定关系。";
        }
        LocalDate targetDate = date == null ? LocalDate.now() : date;
        List<UserActivityDTO> activities = activityParticipationMapper.selectUserActivitiesByElderIdAndDate(elderId,
                targetDate);
        List<String> menuItems = collectMenus(targetDate);
        List<Announcement> announcements = announcementMapper.selectByConditions(3, null, Boolean.TRUE);
        LocalDateTime from = targetDate.atStartOfDay();
        LocalDateTime to = targetDate.plusDays(1).atStartOfDay().minusSeconds(1);
        List<VisitAppointment> visits = visitAppointmentMapper.selectByConditions(elderId, null, from, to);

        String activityText = (activities == null || activities.isEmpty())
                ? "无活动安排"
                : activities.stream()
                        .map(item -> item.getActivityName() + "(" + item.getStartTime() + "-" + item.getEndTime() + ")")
                        .collect(Collectors.joining("、"));
        String menuText = menuItems.isEmpty() ? "无菜单信息" : String.join("、", menuItems);
        String announcementText = (announcements == null || announcements.isEmpty())
                ? "无公告"
                : announcements.stream().map(Announcement::getTitle).collect(Collectors.joining("、"));
        String visitText = (visits == null || visits.isEmpty())
                ? "无探访预约"
                : visits.stream().map(v -> v.getVisitorName() + "(" + v.getStatus() + ")")
                        .collect(Collectors.joining("、"));
        return "日期:" + targetDate + "\n活动:" + activityText + "\n菜单:" + menuText + "\n公告:" + announcementText + "\n探访:"
                + visitText;
    }

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
        List<FamilyElderRelation> primaryRelations = relations.stream()
                .filter(item -> Boolean.TRUE.equals(item.getIsPrimaryContact()))
                .toList();
        if (primaryRelations.size() == 1) {
            return primaryRelations.get(0).getElderId();
        }
        if (relations.size() == 1) {
            return relations.get(0).getElderId();
        }
        return null;
    }

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
        Map<Long, FamilyElderRelation> relationByElder = relations.stream()
                .collect(Collectors.toMap(FamilyElderRelation::getElderId, item -> item, (a, b) -> a,
                        LinkedHashMap::new));
        List<String> elderSummaries = relationByElder.values().stream()
                .sorted(Comparator.comparing(FamilyElderRelation::getIsPrimaryContact,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(FamilyElderRelation::getElderId))
                .map(rel -> {
                    ElderlyUser elder = elderlyUserMapper.selectById(rel.getElderId());
                    if (elder == null) {
                        return "ID:" + rel.getElderId();
                    }
                    String name = elder.getFullName() == null ? "未知" : elder.getFullName();
                    return "ID:" + elder.getId() + " 姓名:" + name
                            + (Boolean.TRUE.equals(rel.getIsPrimaryContact()) ? "(默认)" : "");
                })
                .toList();
        Long defaultElderId = resolveDefaultElderId(familyPhone);
        if (defaultElderId != null) {
            return "已绑定老人:" + String.join("；", elderSummaries) + "。默认老人ID=" + defaultElderId + "，未指定老人时优先使用该对象。";
        }
        return "已绑定老人:" + String.join("；", elderSummaries) + "。当前存在多位绑定老人，请优先让用户提供姓名或手机号后4位。";
    }

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

    private String normalizePhone(String phone) {
        return phone == null ? null : phone.trim();
    }
}
