package com.example.project.mapper;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.project.pojo.entity.MedicalTeamDuty;

@Mapper
public interface MedicalTeamDutyMapper {

    int insert(MedicalTeamDuty row);

    int updateById(MedicalTeamDuty row);

    int deleteById(@Param("id") Long id);

    MedicalTeamDuty selectById(@Param("id") Long id);

    List<MedicalTeamDuty> selectAll();

    List<MedicalTeamDuty> selectByDeptAndDutyDate(@Param("deptId") Long deptId,
            @Param("dutyDate") LocalDate dutyDate);
}
