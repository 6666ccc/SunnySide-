package cn.lc.sunnyside.mapper;

import cn.lc.sunnyside.POJO.DO.VitalSigns;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface VitalSignsMapper {

    List<VitalSigns> selectByPatientIdAndDateRange(@Param("patientId") Long patientId,
                                                   @Param("startDate") LocalDate startDate,
                                                   @Param("endDate") LocalDate endDate);

    List<VitalSigns> selectLatestByPatientId(@Param("patientId") Long patientId,
                                             @Param("limit") int limit);
}
