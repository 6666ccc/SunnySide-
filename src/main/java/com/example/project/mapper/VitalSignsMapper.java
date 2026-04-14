package com.example.project.mapper;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.project.pojo.entity.VitalSigns;

@Mapper
public interface VitalSignsMapper {

    int insert(VitalSigns row);

    int updateById(VitalSigns row);

    int deleteById(@Param("id") Long id);

    VitalSigns selectById(@Param("id") Long id);

    List<VitalSigns> selectAll();

    List<VitalSigns> selectByPatientAndRecordDate(@Param("patientId") Long patientId,
            @Param("recordDate") LocalDate recordDate);
}
