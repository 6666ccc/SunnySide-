package cn.lc.sunnyside.mapper;

import cn.lc.sunnyside.POJO.DO.ActivitySchedule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface ActivityScheduleMapper {

    /**
     * 查询指定日期活动计划。
     *
     * @param date 日期
     * @return 活动列表
     */
    List<ActivitySchedule> selectByDate(@Param("date") LocalDate date);

    /**
     * 按主键查询活动计划。
     *
     * @param id 活动ID
     * @return 活动实体
     */
    ActivitySchedule selectById(@Param("id") Long id);
}
