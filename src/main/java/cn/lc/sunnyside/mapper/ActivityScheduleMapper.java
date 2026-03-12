package cn.lc.sunnyside.mapper;

import cn.lc.sunnyside.POJO.DO.ActivitySchedule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface ActivityScheduleMapper {

    List<ActivitySchedule> selectByDate(@Param("date") LocalDate date);

    ActivitySchedule selectById(@Param("id") Long id);
}
