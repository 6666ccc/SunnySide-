package com.example.project.pojo.entity;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class HospitalDepartment {
    private Long id;
    private String deptName;
    private String contactPhone;
    private String location;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
