package cn.lc.sunnyside.mapper;

import cn.lc.sunnyside.POJO.DO.TreatmentPlan;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface TreatmentPlanMapper {

    List<TreatmentPlan> selectByPatientIdAndDate(@Param("patientId") Long patientId,
                                                 @Param("planDate") LocalDate planDate);

    TreatmentPlan selectById(@Param("id") Long id);
}
