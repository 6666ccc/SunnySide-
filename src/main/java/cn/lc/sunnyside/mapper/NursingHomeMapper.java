package cn.lc.sunnyside.mapper;

import cn.lc.sunnyside.POJO.DO.NursingHome;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface NursingHomeMapper {

    /**
     * 按主键查询养老院信息。
     *
     * @param id 养老院ID
     * @return 养老院实体
     */
    NursingHome selectById(@Param("id") Long id);
}
