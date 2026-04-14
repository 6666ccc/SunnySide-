package com.example.project.mapper;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.project.pojo.entity.TreatmentPlan;

@Mapper
public interface TreatmentPlanMapper {

    int insert(TreatmentPlan row);

    int updateById(TreatmentPlan row);

    int deleteById(@Param("id") Long id);

    TreatmentPlan selectById(@Param("id") Long id);

    List<TreatmentPlan> selectAll();

    List<TreatmentPlan> selectByPatientAndPlanDate(@Param("patientId") Long patientId,
            @Param("planDate") LocalDate planDate);

    /**
     * 按患者与计划日期范围查询，排除纯用餐类（MEAL），用于「治疗/检查」类汇总。
     */
    List<TreatmentPlan> selectByPatientPlanDateRangeExcludingMeal(@Param("patientId") Long patientId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
