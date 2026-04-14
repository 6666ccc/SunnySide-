package com.example.project.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.project.mapper.HospitalDepartmentMapper;
import com.example.project.pojo.entity.HospitalDepartment;
import com.example.project.service.HospitalDepartmentService;

/* 医院部门服务实现类 */
@Service
public class HospitalDepartmentServiceImpl implements HospitalDepartmentService {

    @Autowired
    private HospitalDepartmentMapper hospitalDepartmentMapper;

    /* 保存医院部门 */
    @Override
    public int save(HospitalDepartment row) {
        return hospitalDepartmentMapper.insert(row);
    }

    /* 更新医院部门 */
    @Override
    public int update(HospitalDepartment row) {
        return hospitalDepartmentMapper.updateById(row);
    }

    @Override
    public int removeById(Long id) {
        return hospitalDepartmentMapper.deleteById(id);
    }

    /* 根据ID获取医院部门 */
    @Override
    public HospitalDepartment getById(Long id) {
        return hospitalDepartmentMapper.selectById(id);
    }

    /* 获取所有医院部门 */
    @Override
    public List<HospitalDepartment> listAll() {
        return hospitalDepartmentMapper.selectAll();
    }
}
