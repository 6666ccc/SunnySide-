package cn.lc.sunnyside.mapper;

import cn.lc.sunnyside.POJO.DO.MedicalTeamDuty;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface MedicalTeamDutyMapper {

    List<MedicalTeamDuty> selectByDeptIdAndDate(@Param("deptId") Long deptId,
                                                @Param("date") LocalDate date);
}
