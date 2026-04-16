package com.example.project.pojo.vo;

import lombok.Data;

/**
 * 按住院号搜索患者时的返回（可经服务层脱敏后返回前端）。
 */
@Data
public class PatientSearchVo {

    private Long patientId;
    private String patientName;
    private String bedNumber;
    private String admissionNo;
    private String deptName;
}
