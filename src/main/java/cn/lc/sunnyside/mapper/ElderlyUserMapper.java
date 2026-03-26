package cn.lc.sunnyside.mapper;

import cn.lc.sunnyside.POJO.DO.ElderlyUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ElderlyUserMapper {

    /**
     * 按主键查询老人信息。
     *
     * @param id 老人ID
     * @return 老人实体
     */
    ElderlyUser selectById(@Param("id") Long id);

    /**
     * 按姓氏查询老人信息。
     *
     * @param surname 姓氏
     * @return 老人实体
     */
    ElderlyUser selectBySurname(@Param("surname") String surname);

    /**
     * 按身份线索（姓名/手机号尾号等）查询候选老人列表。
     *
     * @param ref 身份线索
     * @return 候选老人列表
     */
    java.util.List<ElderlyUser> selectByRef(@Param("ref") String ref);
}
