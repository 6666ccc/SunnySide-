package com.example.project.service;

import java.util.List;

import com.example.project.pojo.entity.Patient;
import com.example.project.pojo.vo.PatientBasicInfoVo;

public interface PatientService {

    int save(Patient row);

    int update(Patient row);

    int removeById(Long id);

    Patient getById(Long id);

    List<Patient> listAll();

    PatientBasicInfoVo getBasicInfoWithDept(Long patientId);
}
