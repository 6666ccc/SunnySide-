package cn.lc.sunnyside.Service.ServiceImpl;

import cn.lc.sunnyside.POJO.DO.Menu;
import cn.lc.sunnyside.Service.MenuService;
import cn.lc.sunnyside.mapper.MenuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MenuServiceImpl implements MenuService {

    private final MenuMapper menuMapper;

    @Override
    public List<Menu> getCurrentMenu() {
        LocalTime now = LocalTime.now();
        String mealType;

        if (now.isBefore(LocalTime.of(9, 0))) {
            mealType = "BREAKFAST";
        } else if (now.isBefore(LocalTime.of(14, 0))) {
            mealType = "LUNCH";
        } else if (now.isBefore(LocalTime.of(19, 0))) {
            mealType = "DINNER";
        } else {
            mealType = "SNACK";
        }

        return menuMapper.selectByDateAndType(LocalDate.now(), mealType);
    }

    @Override
    public List<Menu> getMenuByDateAndMeal(LocalDate date, String mealType) {
        return menuMapper.selectByDateAndType(date, mealType);
    }
}
