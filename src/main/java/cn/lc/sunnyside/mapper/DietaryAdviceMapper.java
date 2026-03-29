package cn.lc.sunnyside.mapper;

import cn.lc.sunnyside.POJO.DO.DietaryAdvice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface DietaryAdviceMapper {

    List<DietaryAdvice> selectByPatientIdAndDate(@Param("patientId") Long patientId,
                                                 @Param("mealDate") LocalDate mealDate);

    List<DietaryAdvice> selectByPatientIdAndDateAndType(@Param("patientId") Long patientId,
                                                       @Param("mealDate") LocalDate mealDate,
                                                       @Param("mealType") String mealType);
}
