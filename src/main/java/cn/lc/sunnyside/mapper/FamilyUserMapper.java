package cn.lc.sunnyside.mapper;

import cn.lc.sunnyside.POJO.DO.FamilyUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FamilyUserMapper {

    FamilyUser selectById(@Param("id") Long id);

    FamilyUser selectByPhone(@Param("phone") String phone);

    FamilyUser selectByUsername(@Param("username") String username);
}
