package cn.lc.sunnyside.mapper;

import cn.lc.sunnyside.POJO.DO.RelativeUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RelativeUserMapper {

    RelativeUser selectById(@Param("id") Long id);

    RelativeUser selectByPhone(@Param("phone") String phone);

    RelativeUser selectByUsername(@Param("username") String username);
}
