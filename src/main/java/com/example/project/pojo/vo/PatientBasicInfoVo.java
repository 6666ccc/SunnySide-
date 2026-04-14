package com.example.project.pojo.vo;

import java.time.LocalDate;

import lombok.Data;

@Data
public class PatientBasicInfoVo {
    private Long patientId;
    private String patientName;
    private String gender;
    private String admissionNo;
    private String bedNumber;
    private Long deptId;
    private String deptName;
    private String deptContactPhone;
    private String deptLocation;
    private LocalDate admissionDate;
    private LocalDate dischargeDate;
    private String status;
}
