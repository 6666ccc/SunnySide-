package cn.lc.sunnyside.mapper;

import cn.lc.sunnyside.POJO.DO.MedicalDuty;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface MedicalDutyMapper {

    /**
     * 查询指定日期值班医护人员。
     *
     * @param date 日期
     * @return 值班列表
     */
    List<MedicalDuty> selectOnDuty(@Param("date") LocalDate date);
}
