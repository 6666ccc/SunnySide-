package com.example.project.service.impl;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.project.mapper.VitalSignsMapper;
import com.example.project.pojo.entity.VitalSigns;
import com.example.project.service.VitalSignsService;

/* 生命体征服务实现类 */
@Service
public class VitalSignsServiceImpl implements VitalSignsService {

    @Autowired
    private VitalSignsMapper vitalSignsMapper;

    /* 保存生命体征 */
    @Override
    public int save(VitalSigns row) {
        return vitalSignsMapper.insert(row);
    }

    /* 更新生命体征 */
    @Override
    public int update(VitalSigns row) {
        return vitalSignsMapper.updateById(row);
    }

    /* 删除生命体征 */
    @Override
    public int removeById(Long id) {
        return vitalSignsMapper.deleteById(id);
    }

    /* 根据ID获取生命体征 */
    @Override
    public VitalSigns getById(Long id) {
        return vitalSignsMapper.selectById(id);
    }

    /* 获取所有生命体征 */
    @Override
    public List<VitalSigns> listAll() {
        return vitalSignsMapper.selectAll();
    }

    /* 根据患者ID和记录日期获取生命体征 */
    @Override
    public List<VitalSigns> listByPatientAndRecordDate(Long patientId, LocalDate recordDate) {
        return vitalSignsMapper.selectByPatientAndRecordDate(patientId, recordDate);
    }

    @Override
    public List<VitalSigns> listByPatientAndDateRange(Long patientId, LocalDate startInclusive,
            LocalDate endInclusive) {
        return vitalSignsMapper.selectByPatientAndDateRange(patientId, startInclusive, endInclusive);
    }
}
