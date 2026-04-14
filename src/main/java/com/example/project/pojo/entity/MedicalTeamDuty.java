package com.example.project.pojo.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class MedicalTeamDuty {
    private Long id;
    private Long deptId;
    private LocalDate dutyDate;
    private String staffName;
    /** CHIEF_DOCTOR, ATTENDING_DOCTOR, PRIMARY_NURSE, CAREGIVER */
    private String staffRole;
    private String dutyTime;
    private String phone;
    private LocalDateTime createdAt;
}
