package com.example.project.service;

import java.time.LocalDate;
import java.util.List;

import com.example.project.pojo.entity.VitalSigns;

public interface VitalSignsService {

    int save(VitalSigns row);

    int update(VitalSigns row);

    int removeById(Long id);

    VitalSigns getById(Long id);

    List<VitalSigns> listAll();

    List<VitalSigns> listByPatientAndRecordDate(Long patientId, LocalDate recordDate);

    List<VitalSigns> listByPatientAndDateRange(Long patientId, LocalDate startInclusive, LocalDate endInclusive);
}
