package cn.lc.sunnyside.POJO.DO;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 活动日程表
 */
@Data
public class ActivitySchedule {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 所属养老院ID
     */
    private Long nursingHomeId;

    /**
     * 活动名称
     */
    private String activityName;

    /**
     * 活动描述
     */
    private String description;

    /**
     * 活动日期
     */
    private LocalDate activityDate;

    /**
     * 开始时间
     */
    private LocalTime startTime;

    /**
     * 结束时间
     */
    private LocalTime endTime;

    /**
     * 活动地点
     */
    private String location;

    /**
     * 活动类型 ('EXERCISE', 'MEAL', 'ENTERTAINMENT', 'HEALTH_CHECK', 'SOCIAL', 'OTHER')
     */
    private String category;

    /**
     * 容纳人数
     */
    private Integer capacity;

    /**
     * 是否必参
     */
    private Boolean isMandatory;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
