package com.example.project.service;

import java.util.List;

import com.example.project.pojo.entity.HospitalDepartment;

public interface HospitalDepartmentService {

    int save(HospitalDepartment row);

    int update(HospitalDepartment row);

    int removeById(Long id);

    HospitalDepartment getById(Long id);

    List<HospitalDepartment> listAll();
}
