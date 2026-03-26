package cn.lc.sunnyside.mapper;

import cn.lc.sunnyside.POJO.DO.ActivityParticipation;
import cn.lc.sunnyside.POJO.DTO.UserActivityDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface ActivityParticipationMapper {

    /**
     * 查询老人某日的活动报名记录。
     *
     * @param elderId 老人ID
     * @param date 日期
     * @return 报名记录列表
     */
    List<ActivityParticipation> selectByElderIdAndDate(@Param("elderId") Long elderId, @Param("date") LocalDate date);

    /**
     * 查询老人某日活动视图数据。
     *
     * @param elderId 老人ID
     * @param date 日期
     * @return 活动展示DTO列表
     */
    List<UserActivityDTO> selectUserActivitiesByElderIdAndDate(@Param("elderId") Long elderId, @Param("date") LocalDate date);

    /**
     * 新增活动报名记录。
     *
     * @param activityParticipation 报名实体
     * @return 受影响行数
     */
    int insert(ActivityParticipation activityParticipation);

    /**
     * 取消活动报名。
     *
     * @param elderId 老人ID
     * @param activityId 活动ID
     * @return 受影响行数
     */
    int cancelRegistration(@Param("elderId") Long elderId, @Param("activityId") Long activityId);
}
