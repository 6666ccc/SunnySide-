package cn.lc.sunnyside.mapper;

import cn.lc.sunnyside.POJO.DO.HealthRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface HealthRecordMapper {

    /**
     * 查询指定老人在日期区间内的健康记录。
     *
     * @param elderId 老人ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 健康记录列表
     */
    List<HealthRecord> selectByElderIdAndDateRange(@Param("elderId") Long elderId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * 查询指定老人最近若干条健康记录。
     *
     * @param elderId 老人ID
     * @param limit 返回条数
     * @return 健康记录列表
     */
    List<HealthRecord> selectLatestByElderIdLimit(@Param("elderId") Long elderId,
            @Param("limit") int limit);
}
