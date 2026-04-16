package com.example.project.pojo.dto;

import lombok.Data;

@Data
public class PatientRegisterRequest {

    /** 住院号（验证患者身份） */
    private String admissionNo;

    private String username;

    private String password;
}
