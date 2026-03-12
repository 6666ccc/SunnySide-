package cn.lc.sunnyside.mapper;

import cn.lc.sunnyside.POJO.DO.ElderlyUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ElderlyUserMapper {

    ElderlyUser selectById(@Param("id") Long id);
    ElderlyUser selectBySurname(@Param("surname") String surname);
}
