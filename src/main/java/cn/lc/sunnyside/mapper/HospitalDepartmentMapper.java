package cn.lc.sunnyside.mapper;

import cn.lc.sunnyside.POJO.DO.HospitalDepartment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface HospitalDepartmentMapper {

    HospitalDepartment selectById(@Param("id") Long id);
}
