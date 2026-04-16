package com.example.project.pojo.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class Patient {
    private Long id;
    private Long deptId;
    private String patientName;
    /** MALE / FEMALE / OTHER */
    private String gender;
    private String admissionNo;
    private String bedNumber;
    private LocalDate admissionDate;
    private LocalDate dischargeDate;
    /** IN_HOSPITAL / DISCHARGED / TRANSFERRED */
    private String status;
    private String username;
    private String password;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
