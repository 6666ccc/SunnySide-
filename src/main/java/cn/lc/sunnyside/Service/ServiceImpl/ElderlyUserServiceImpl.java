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

    /**
     * 获取老人当前位置（通过查询老人当天的活动安排来推断）
     *
     * @param elderId 老人ID
     * @return 包含老人当天活动信息的字符串
     */
    @Override
    public String getElderLocation(Long elderId) {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        // 查询老人当天的所有活动参与记录
        List<UserActivityDTO> userActivities = activityParticipationMapper.selectUserActivitiesByElderIdAndDate(elderId,
                today);
        log.info("查询老人ID={}当天活动参与记录，结果={}", elderId, userActivities);

        return userActivities.toString();
    }

    /**
     * 获取老人的健康提醒信息
     *
     * @param elderId 老人ID
     * @return 健康提醒的文本内容
     */
    @Override
    public String getHealthReminder(Long elderId) {
        return "记得喝水吃药哦！";
    }

    /**
     * 根据老人ID获取老人详细信息
     *
     * @param id 老人唯一标识
     * @return 老人实体对象，如果不存在则返回 null
     */
    @Override
    public ElderlyUser getById(Long id) {
        return elderlyUserMapper.selectById(id);
    }

    /**
     * 根据老人姓氏查询老人信息
     * 
     * @param surname 老人姓氏
     * @return 老人信息
     */
    @Override
    public ElderlyUser findElderlyUserBySurname(String surname) {
        return elderlyUserMapper.selectBySurname(surname);
    }

    /**
     * 根据身份线索查询匹配的老人列表，支持姓名、手机号后4位等模糊匹配
     * 
     * @param ref 身份线索
     * @return 匹配的老人列表
     */
    @Override
    public List<ElderlyUser> findByRef(String ref) {
        if (ref == null || ref.isBlank()) {
            return List.of();
        }
        return elderlyUserMapper.selectByRef(ref.trim());
    }
}
