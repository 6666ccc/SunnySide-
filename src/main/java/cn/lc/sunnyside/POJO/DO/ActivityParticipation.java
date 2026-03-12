package cn.lc.sunnyside.POJO.DO;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 活动参与记录表
 */
@Data
public class ActivityParticipation {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 老人ID
     */
    private Long elderId;

    /**
     * 活动ID
     */
    private Long activityId;

    /**
     * 参与状态 ('REGISTERED', 'ATTENDED', 'ABSENT', 'CANCELED')
     */
    private String participationStatus;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 活动开始时间
     */
    private LocalTime activityDateStart;

    /**
     * 活动结束时间
     */
    private LocalTime activityDateEnd;

    /**
     * 活动日期
     */
    private LocalDate activityDate;
}
