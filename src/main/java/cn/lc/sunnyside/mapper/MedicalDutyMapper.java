package cn.lc.sunnyside.mapper;

import cn.lc.sunnyside.POJO.DO.MedicalDuty;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface MedicalDutyMapper {

    List<MedicalDuty> selectOnDuty(@Param("date") LocalDate date);
}
