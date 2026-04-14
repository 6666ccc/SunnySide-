package com.example.project.service.impl;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.project.mapper.DietaryAdviceMapper;
import com.example.project.pojo.entity.DietaryAdvice;
import com.example.project.service.DietaryAdviceService;

/* 膳食建议服务实现类 */
@Service
public class DietaryAdviceServiceImpl implements DietaryAdviceService {

    @Autowired
    private DietaryAdviceMapper dietaryAdviceMapper;

    /* 保存膳食建议 */
    @Override
    public int save(DietaryAdvice row) {
        return dietaryAdviceMapper.insert(row);
    }

    /* 更新膳食建议 */
    @Override
    public int update(DietaryAdvice row) {
        return dietaryAdviceMapper.updateById(row);
    }

    /* 删除膳食建议 */
    @Override
    public int removeById(Long id) {
        return dietaryAdviceMapper.deleteById(id);
    }

    /* 根据ID获取膳食建议 */
    @Override
    public DietaryAdvice getById(Long id) {
        return dietaryAdviceMapper.selectById(id);
    }

    /* 获取所有膳食建议 */
    @Override
    public List<DietaryAdvice> listAll() {
        return dietaryAdviceMapper.selectAll();
    }

    /* 根据患者ID和餐次日期获取膳食建议 */
    @Override
    public List<DietaryAdvice> listByPatientAndMealDate(Long patientId, LocalDate mealDate) {
        return dietaryAdviceMapper.selectByPatientAndMealDate(patientId, mealDate);
    }
}
