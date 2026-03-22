package cn.lc.sunnyside.Service.ServiceImpl;

import cn.lc.sunnyside.POJO.DO.ElderlyUser;
import cn.lc.sunnyside.POJO.DTO.UserActivityDTO;
import cn.lc.sunnyside.Service.ElderlyUserService;
import cn.lc.sunnyside.mapper.ActivityParticipationMapper;
import cn.lc.sunnyside.mapper.ElderlyUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ElderlyUserServiceImpl implements ElderlyUserService {

    private final ElderlyUserMapper elderlyUserMapper;
    private final ActivityParticipationMapper activityParticipationMapper;

    @Override
    public String getElderLocation(Long elderId) {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        List<UserActivityDTO> userActivities = activityParticipationMapper.selectUserActivitiesByElderIdAndDate(elderId, today);
        log.info("查询老人ID={}当天活动参与记录，结果={}", elderId, userActivities);

        return userActivities.toString();
    }

    @Override
    public String getHealthReminder(Long elderId) {
        return "记得喝水吃药哦！";
    }

    @Override
    public ElderlyUser getById(Long id) {
        return elderlyUserMapper.selectById(id);
    }



    /**
     * 根据老人姓氏查询老人信息
     * @param surname 老人姓氏
     * @return 老人信息
     */
    @Override
    public ElderlyUser findElderlyUserBySurname(String surname) {
        return elderlyUserMapper.selectBySurname(surname);
    }

    @Override
    public List<ElderlyUser> findByRef(String ref) {
        if (ref == null || ref.isBlank()) {
            return List.of();
        }
        return elderlyUserMapper.selectByRef(ref.trim());
    }
}
