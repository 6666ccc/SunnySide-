package cn.lc.sunnyside.mapper;

import cn.lc.sunnyside.POJO.DO.Menu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface MenuMapper {

    /**
     * 按日期与餐次查询菜单。
     *
     * @param date 日期
     * @param type 餐次类型
     * @return 菜单列表
     */
    List<Menu> selectByDateAndType(@Param("date") LocalDate date, @Param("type") String type);
}
