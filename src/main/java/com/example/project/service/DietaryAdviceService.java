package com.example.project.service;

import java.time.LocalDate;
import java.util.List;

import com.example.project.pojo.entity.DietaryAdvice;

public interface DietaryAdviceService {

    int save(DietaryAdvice row);

    int update(DietaryAdvice row);

    int removeById(Long id);

    DietaryAdvice getById(Long id);

    List<DietaryAdvice> listAll();

    List<DietaryAdvice> listByPatientAndMealDate(Long patientId, LocalDate mealDate);
}
