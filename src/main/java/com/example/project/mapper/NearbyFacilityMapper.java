package com.example.project.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.project.pojo.entity.NearbyFacility;

@Mapper
public interface NearbyFacilityMapper {

    List<NearbyFacility> selectByDeptId(@Param("deptId") Long deptId);
}
