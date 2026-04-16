package com.example.project.pojo.dto;

import lombok.Data;

/**
 * 家属按患者ID绑定患者请求体。
 */
@Data
public class BindPatientRequest {

    /** 患者注册后的唯一ID */
    private Long patientId;

    /** 与患者关系，如：配偶、子女、父母 */
    private String relationType;

    /** 是否为法律授权代理人/主要陪护，默认 false */
    private Boolean isLegalProxy;
}
