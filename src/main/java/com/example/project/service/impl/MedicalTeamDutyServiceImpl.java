package com.example.project.service.impl;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.project.mapper.MedicalTeamDutyMapper;
import com.example.project.pojo.entity.MedicalTeamDuty;
import com.example.project.service.MedicalTeamDutyService;

/* 医疗团队职责服务实现类 */
@Service
public class MedicalTeamDutyServiceImpl implements MedicalTeamDutyService {

    @Autowired
    private MedicalTeamDutyMapper medicalTeamDutyMapper;

    /* 保存医疗团队职责 */
    @Override
    public int save(MedicalTeamDuty row) {
        return medicalTeamDutyMapper.insert(row);
    }

    /* 更新医疗团队职责 */
    @Override
    public int update(MedicalTeamDuty row) {
        return medicalTeamDutyMapper.updateById(row);
    }

    /* 删除医疗团队职责 */
    @Override
    public int removeById(Long id) {
        return medicalTeamDutyMapper.deleteById(id);
    }

    /* 根据ID获取医疗团队职责 */
    @Override
    public MedicalTeamDuty getById(Long id) {
        return medicalTeamDutyMapper.selectById(id);
    }

    /* 获取所有医疗团队职责 */
    @Override
    public List<MedicalTeamDuty> listAll() {
        return medicalTeamDutyMapper.selectAll();
    }

    /* 根据科室ID和职责日期获取医疗团队职责 */
    @Override
    public List<MedicalTeamDuty> listByDeptAndDutyDate(Long deptId, LocalDate dutyDate) {
        return medicalTeamDutyMapper.selectByDeptAndDutyDate(deptId, dutyDate);
    }
}
