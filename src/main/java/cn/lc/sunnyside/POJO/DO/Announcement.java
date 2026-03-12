package cn.lc.sunnyside.POJO.DO;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 通知公告表
 */
@Data
public class Announcement {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 所属养老院ID
     */
    private Long nursingHomeId;

    /**
     * 公告标题
     */
    private String title;

    /**
     * 公告内容
     */
    private String content;

    /**
     * 发布日期
     */
    private LocalDate announcementDate;

    /**
     * 优先级 ('LOW', 'MEDIUM', 'HIGH')
     */
    private String priority;

    /**
     * 是否有效
     */
    private Boolean isActive;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
