package com.example.project.mapper;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.project.pojo.entity.DietaryAdvice;

@Mapper
public interface DietaryAdviceMapper {

    int insert(DietaryAdvice row);

    int updateById(DietaryAdvice row);

    int deleteById(@Param("id") Long id);

    DietaryAdvice selectById(@Param("id") Long id);

    List<DietaryAdvice> selectAll();

    List<DietaryAdvice> selectByPatientAndMealDate(@Param("patientId") Long patientId,
            @Param("mealDate") LocalDate mealDate);
}
