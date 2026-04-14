package com.example.project.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.project.mapper.PatientMapper;
import com.example.project.pojo.entity.Patient;
import com.example.project.pojo.vo.PatientBasicInfoVo;
import com.example.project.service.PatientService;

/* 患者服务实现类 */
@Service
public class PatientServiceImpl implements PatientService {

    @Autowired
    private PatientMapper patientMapper;

    /* 保存患者 */
    @Override
    public int save(Patient row) {
        return patientMapper.insert(row);
    }

    /* 更新患者 */
    @Override
    public int update(Patient row) {
        return patientMapper.updateById(row);
    }

    /* 删除患者 */
    @Override
    public int removeById(Long id) {
        return patientMapper.deleteById(id);
    }

    /* 根据ID获取患者 */
    @Override
    public Patient getById(Long id) {
        return patientMapper.selectById(id);
    }

    /* 获取所有患者 */
    @Override
    public List<Patient> listAll() {
        return patientMapper.selectAll();
    }

    /* 根据患者ID获取患者基本信息和科室信息 */
    @Override
    public PatientBasicInfoVo getBasicInfoWithDept(Long patientId) {
        return patientMapper.selectBasicInfoWithDept(patientId);
    }
}
