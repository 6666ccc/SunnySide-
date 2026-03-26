package cn.lc.sunnyside.mapper;

import cn.lc.sunnyside.POJO.DO.FamilyUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FamilyUserMapper {

    /**
     * 按主键查询家属用户。
     *
     * @param id 家属ID
     * @return 家属实体
     */
    FamilyUser selectById(@Param("id") Long id);

    /**
     * 按手机号查询家属用户。
     *
     * @param phone 家属手机号
     * @return 家属实体
     */
    FamilyUser selectByPhone(@Param("phone") String phone);

    /**
     * 按用户名查询家属用户。
     *
     * @param username 用户名
     * @return 家属实体
     */
    FamilyUser selectByUsername(@Param("username") String username);
}
