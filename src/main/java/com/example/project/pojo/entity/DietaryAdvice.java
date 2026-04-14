package com.example.project.pojo.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class DietaryAdvice {
    private Long id;
    private Long patientId;
    private LocalDate mealDate;
    /** BREAKFAST, LUNCH, DINNER, SNACK */
    private String mealType;
    private String foodContent;
    private String nutritionNotes;
    private LocalDateTime createdAt;
}
