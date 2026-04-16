package com.example.project.service;

import java.time.LocalDate;
import java.util.List;

import com.example.project.pojo.entity.TreatmentPlan;

public interface TreatmentPlanService {

    int save(TreatmentPlan row);

    int update(TreatmentPlan row);

    int removeById(Long id);

    TreatmentPlan getById(Long id);

    List<TreatmentPlan> listAll();

    List<TreatmentPlan> listByPatientAndPlanDate(Long patientId, LocalDate planDate);

    /**
     * 查询日期范围内非用餐类诊疗计划（手术、检查、输液、用药、护理等），用于家属了解「要做哪些检查/治疗」。
     */
    List<TreatmentPlan> listTreatmentAndExaminationByDateRange(Long patientId, LocalDate startInclusive,
            LocalDate endInclusive);

    /** 未完成且非 MEAL 的诊疗计划项 */
    List<TreatmentPlan> listUncompletedByPatientExcludingMeal(Long patientId);
}
