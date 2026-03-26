package cn.lc.sunnyside.Service;

import cn.lc.sunnyside.POJO.DO.Menu;

import java.time.LocalDate;
import java.util.List;

/**
 * 菜单查询服务抽象。
 */
public interface MenuService {
    /**
     * 查询当前菜单。
     *
     * @return 菜单列表
     */
    List<Menu> getCurrentMenu();

    /**
     * 按日期与餐次查询菜单。
     *
     * @param date 日期
     * @param mealType 餐次
     * @return 菜单列表
     */
    List<Menu> getMenuByDateAndMeal(LocalDate date, String mealType);
}
