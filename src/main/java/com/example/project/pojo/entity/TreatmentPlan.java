package com.example.project.pojo.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import lombok.Data;

@Data
public class TreatmentPlan {
    private Long id;
    private Long patientId;
    private String taskName;
    private String description;
    private LocalDate planDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String location;
    /** SURGERY, EXAMINATION, INFUSION, MEDICATION, MEAL, NURSING, OTHER */
    private String category;
    private Boolean isCompleted;
    private LocalDateTime createdAt;
}
