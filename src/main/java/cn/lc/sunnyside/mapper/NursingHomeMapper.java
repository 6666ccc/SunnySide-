package cn.lc.sunnyside.mapper;

import cn.lc.sunnyside.POJO.DO.NursingHome;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface NursingHomeMapper {

    NursingHome selectById(@Param("id") Long id);
}
