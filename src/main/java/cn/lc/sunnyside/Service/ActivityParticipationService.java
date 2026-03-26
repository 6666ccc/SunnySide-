package cn.lc.sunnyside.Service;

import cn.lc.sunnyside.POJO.DTO.UserActivityDTO;

import java.time.LocalDate;
import java.util.List;

/**
 * 活动报名服务抽象。
 */
public interface ActivityParticipationService {
    /**
     * 报名活动。
     *
     * @param elderId 老人ID
     * @param activityId 活动ID
     * @return 执行结果文案
     */
    String registerActivity(Long elderId, Long activityId);

    /**
     * 取消活动报名。
     *
     * @param elderId 老人ID
     * @param activityId 活动ID
     * @return 执行结果文案
     */
    String cancelActivityRegistration(Long elderId, Long activityId);

    /**
     * 查询老人某日报名活动。
     *
     * @param elderId 老人ID
     * @param date 日期
     * @return 活动视图列表
     */
    List<UserActivityDTO> getMyActivityRegistrations(Long elderId, LocalDate date);
}
