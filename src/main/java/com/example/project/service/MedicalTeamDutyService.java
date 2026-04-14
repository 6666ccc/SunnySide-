package com.example.project.service;

import java.time.LocalDate;
import java.util.List;

import com.example.project.pojo.entity.MedicalTeamDuty;

public interface MedicalTeamDutyService {

    int save(MedicalTeamDuty row);

    int update(MedicalTeamDuty row);

    int removeById(Long id);

    MedicalTeamDuty getById(Long id);

    List<MedicalTeamDuty> listAll();

    List<MedicalTeamDuty> listByDeptAndDutyDate(Long deptId, LocalDate dutyDate);
}
