package com.example.project.pojo.vo;

import lombok.Data;

@Data
public class AuthorizedPatientVo {
    private Long patientId;
    private String patientName;
    private String bedNumber;
    private String admissionNo;
    private String relationType;
    private Boolean isLegalProxy;
    private Long deptId;
    private String deptName;
}
