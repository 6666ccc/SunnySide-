package cn.lc.sunnyside.Service.ServiceImpl;

import cn.lc.sunnyside.POJO.DO.Announcement;
import cn.lc.sunnyside.POJO.DO.FamilyUser;
import cn.lc.sunnyside.POJO.DO.Menu;
import cn.lc.sunnyside.POJO.DO.VisitAppointment;
import cn.lc.sunnyside.POJO.DTO.UserActivityDTO;
import cn.lc.sunnyside.mapper.ActivityParticipationMapper;
import cn.lc.sunnyside.mapper.AnnouncementMapper;
import cn.lc.sunnyside.mapper.FamilyElderRelationMapper;
import cn.lc.sunnyside.mapper.FamilyUserMapper;
import cn.lc.sunnyside.mapper.MenuMapper;
import cn.lc.sunnyside.mapper.VisitAppointmentMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FamilyAccessServiceImplTest {

    @Mock
    private FamilyUserMapper familyUserMapper;

    @Mock
    private FamilyElderRelationMapper familyElderRelationMapper;

    @Mock
    private ActivityParticipationMapper activityParticipationMapper;

    @Mock
    private MenuMapper menuMapper;

    @Mock
    private AnnouncementMapper announcementMapper;

    @Mock
    private VisitAppointmentMapper visitAppointmentMapper;

    @InjectMocks
    private FamilyAccessServiceImpl familyAccessService;

    @Test
    void canAccessElderShouldReturnFalseWhenFamilyNotFound() {
        when(familyUserMapper.selectByPhone("13800000000")).thenReturn(null);

        boolean result = familyAccessService.canAccessElder("13800000000", 9L);

        assertThat(result).isFalse();
    }

    @Test
    void getElderDailySummaryShouldReturnSummaryWhenAuthorized() {
        FamilyUser user = new FamilyUser();
        user.setId(1L);
        when(familyUserMapper.selectByPhone("13800000000")).thenReturn(user);
        when(familyElderRelationMapper.existsRelation(1L, 9L)).thenReturn(1);

        UserActivityDTO activityDTO = new UserActivityDTO();
        activityDTO.setActivityName("晨练");
        activityDTO.setStartTime(LocalTime.of(8, 0));
        activityDTO.setEndTime(LocalTime.of(8, 30));
        when(activityParticipationMapper.selectUserActivitiesByElderIdAndDate(eq(9L), any(LocalDate.class)))
                .thenReturn(List.of(activityDTO));

        Menu menu = new Menu();
        menu.setDishName("小米粥");
        when(menuMapper.selectByDateAndType(any(LocalDate.class), eq("BREAKFAST"))).thenReturn(List.of(menu));
        when(menuMapper.selectByDateAndType(any(LocalDate.class), eq("LUNCH"))).thenReturn(List.of());
        when(menuMapper.selectByDateAndType(any(LocalDate.class), eq("DINNER"))).thenReturn(List.of());
        when(menuMapper.selectByDateAndType(any(LocalDate.class), eq("SNACK"))).thenReturn(List.of());

        Announcement announcement = new Announcement();
        announcement.setTitle("防滑提醒");
        when(announcementMapper.selectByConditions(3, null, Boolean.TRUE)).thenReturn(List.of(announcement));

        VisitAppointment visitAppointment = new VisitAppointment();
        visitAppointment.setVisitorName("张三");
        visitAppointment.setStatus("APPROVED");
        visitAppointment.setVisitTime(LocalDateTime.now());
        when(visitAppointmentMapper.selectByConditions(eq(9L), eq(null), any(LocalDateTime.class),
                any(LocalDateTime.class)))
                .thenReturn(List.of(visitAppointment));

        String result = familyAccessService.getElderDailySummary("13800000000", 9L, LocalDate.now());

        assertThat(result).contains("活动:晨练");
        assertThat(result).contains("菜单:BREAKFAST:小米粥");
        assertThat(result).contains("公告:防滑提醒");
        assertThat(result).contains("探访:张三(APPROVED)");
    }
}
