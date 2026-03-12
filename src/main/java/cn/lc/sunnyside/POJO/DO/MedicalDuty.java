package cn.lc.sunnyside.POJO.DO;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 医疗值班表
 */
@Data
public class MedicalDuty {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 所属养老院ID
     */
    private Long nursingHomeId;

    /**
     * 值班日期
     */
    private LocalDate dutyDate;

    /**
     * 值班人员姓名
     */
    private String staffName;

    /**
     * 职位 ('DOCTOR', 'NURSE', 'CAREGIVER')
     */
    private String staffRole;

    /**
     * 值班时段(如: 08:00-16:00)
     */
    private String dutyTime;

    /**
     * 紧急联系电话
     */
    private String phone;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
