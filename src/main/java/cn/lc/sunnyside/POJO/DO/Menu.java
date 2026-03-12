package cn.lc.sunnyside.POJO.DO;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 每日菜单表
 */
@Data
public class Menu {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 所属养老院ID
     */
    private Long nursingHomeId;

    /**
     * 菜单日期
     */
    private LocalDate mealDate;

    /**
     * 餐次类型 ('BREAKFAST', 'LUNCH', 'DINNER', 'SNACK')
     */
    private String mealType;

    /**
     * 菜品名称
     */
    private String dishName;

    /**
     * 营养说明(如: 低盐, 清淡)
     */
    private String nutritionNotes;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
