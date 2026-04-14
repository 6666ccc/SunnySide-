package com.example.project.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.project.pojo.entity.Patient;
import com.example.project.pojo.vo.PatientBasicInfoVo;

@Mapper
public interface PatientMapper {

    int insert(Patient row);

    int updateById(Patient row);

    int deleteById(@Param("id") Long id);

    Patient selectById(@Param("id") Long id);

    List<Patient> selectAll();

    PatientBasicInfoVo selectBasicInfoWithDept(@Param("patientId") Long patientId);
}
