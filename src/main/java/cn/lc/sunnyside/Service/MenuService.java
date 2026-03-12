package cn.lc.sunnyside.Service;

import cn.lc.sunnyside.POJO.DO.Menu;

import java.time.LocalDate;
import java.util.List;

public interface MenuService {
    List<Menu> getCurrentMenu();
    List<Menu> getMenuByDateAndMeal(LocalDate date, String mealType);
}
