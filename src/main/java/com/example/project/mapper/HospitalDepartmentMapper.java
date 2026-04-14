package com.example.project.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.project.pojo.entity.HospitalDepartment;

@Mapper
public interface HospitalDepartmentMapper {

    int insert(HospitalDepartment row);

    int updateById(HospitalDepartment row);

    int deleteById(@Param("id") Long id);

    HospitalDepartment selectById(@Param("id") Long id);

    List<HospitalDepartment> selectAll();
}
