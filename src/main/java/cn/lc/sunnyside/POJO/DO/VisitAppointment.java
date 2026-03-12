package cn.lc.sunnyside.POJO.DO;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 看望老人预约表
 */
@Data
public class VisitAppointment {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 被看望老人ID
     */
    private Long elderId;

    /**
     * 来访人姓名
     */
    private String visitorName;

    /**
     * 来访人联系电话
     */
    private String visitorPhone;

    /**
     * 预约看望时间
     */
    private LocalDateTime visitTime;

    /**
     * 预约状态 ('PENDING', 'APPROVED', 'CANCELED', 'DONE')
     */
    private String status;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 与老人的关系
     */
    private String relation;
}
