package cn.lc.sunnyside.POJO.DO;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 养老院信息表
 */
@Data
public class NursingHome {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 养老院名称
     */
    private String name;

    /**
     * 联系电话
     */
    private String phone;

    /**
     * 地址
     */
    private String address;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
