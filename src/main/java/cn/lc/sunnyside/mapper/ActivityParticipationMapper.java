package cn.lc.sunnyside.mapper;

import cn.lc.sunnyside.POJO.DO.ActivityParticipation;
import cn.lc.sunnyside.POJO.DTO.UserActivityDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface ActivityParticipationMapper {

    List<ActivityParticipation> selectByElderIdAndDate(@Param("elderId") Long elderId, @Param("date") LocalDate date);

    List<UserActivityDTO> selectUserActivitiesByElderIdAndDate(@Param("elderId") Long elderId, @Param("date") LocalDate date);

    int insert(ActivityParticipation activityParticipation);

    int cancelRegistration(@Param("elderId") Long elderId, @Param("activityId") Long activityId);
}
