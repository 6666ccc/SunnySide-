package com.example.project.pojo.vo;

import java.util.List;

import lombok.Data;

/**
 * 当前登录家属（由 JWT 解析）与 {@code relative_patient_relation} 绑定的患者摘要。
 */
@Data
public class RelativeSessionVo {

    /** 亲属表 relative_user 主键 */
    private Long relativeUserId;

    /** JWT subject：当前登录实现为登录名 username */
    private String loginUsername;

    private String fullName;

    private String phone;

    /** 亲属_患者关联表中绑定的患者主键 ID 列表，便于后续工具直接传 patientId */
    private List<Long> boundPatientIds;

    /** 含床位、关系类型等明细 */
    private List<AuthorizedPatientVo> authorizedPatients;
}
