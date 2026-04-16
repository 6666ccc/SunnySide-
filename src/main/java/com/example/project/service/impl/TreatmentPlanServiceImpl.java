package com.example.project.service.impl;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.project.mapper.TreatmentPlanMapper;
import com.example.project.pojo.entity.TreatmentPlan;
import com.example.project.service.TreatmentPlanService;

/* 治疗计划服务实现类 */
@Service
public class TreatmentPlanServiceImpl implements TreatmentPlanService {

    @Autowired
    private TreatmentPlanMapper treatmentPlanMapper;

    /* 保存治疗计划 */
    @Override
    public int save(TreatmentPlan row) {
        return treatmentPlanMapper.insert(row);
    }

    /* 更新治疗计划 */
    @Override
    public int update(TreatmentPlan row) {
        return treatmentPlanMapper.updateById(row);
    }

    /* 删除治疗计划 */
    @Override
    public int removeById(Long id) {
        return treatmentPlanMapper.deleteById(id);
    }

    /* 根据ID获取治疗计划 */
    @Override
    public TreatmentPlan getById(Long id) {
        return treatmentPlanMapper.selectById(id);
    }

    /* 获取所有治疗计划 */
    @Override
    public List<TreatmentPlan> listAll() {
        return treatmentPlanMapper.selectAll();
    }

    /* 根据患者ID和计划日期获取治疗计划 */
    @Override
    public List<TreatmentPlan> listByPatientAndPlanDate(Long patientId, LocalDate planDate) {
        return treatmentPlanMapper.selectByPatientAndPlanDate(patientId, planDate);
    }

    @Override
    public List<TreatmentPlan> listTreatmentAndExaminationByDateRange(Long patientId, LocalDate startInclusive,
            LocalDate endInclusive) {
        return treatmentPlanMapper.selectByPatientPlanDateRangeExcludingMeal(patientId, startInclusive, endInclusive);
    }

    @Override
    public List<TreatmentPlan> listUncompletedByPatientExcludingMeal(Long patientId) {
        return treatmentPlanMapper.selectUncompletedByPatientExcludingMeal(patientId);
    }
}
