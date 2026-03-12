package cn.lc.sunnyside.POJO.DO;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 老人基础信息表
 */
@Data
public class ElderlyUser {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 所属养老院ID
     */
    private Long nursingHomeId;

    /**
     * 姓名
     */
    private String fullName;

    /**
     * 性别 ('MALE', 'FEMALE', 'OTHER')
     */
    private String gender;

    /**
     * 联系电话
     */
    private String phone;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
