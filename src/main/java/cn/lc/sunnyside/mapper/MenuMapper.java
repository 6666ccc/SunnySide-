package cn.lc.sunnyside.mapper;

import cn.lc.sunnyside.POJO.DO.Menu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface MenuMapper {

    List<Menu> selectByDateAndType(@Param("date") LocalDate date, @Param("type") String type);
}
