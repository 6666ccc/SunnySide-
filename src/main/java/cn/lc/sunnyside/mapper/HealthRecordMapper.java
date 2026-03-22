package cn.lc.sunnyside.mapper;

import cn.lc.sunnyside.POJO.DO.HealthRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface HealthRecordMapper {

    List<HealthRecord> selectByElderIdAndDateRange(@Param("elderId") Long elderId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    List<HealthRecord> selectLatestByElderIdLimit(@Param("elderId") Long elderId,
            @Param("limit") int limit);
}
