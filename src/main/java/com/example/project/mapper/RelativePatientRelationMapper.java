package com.example.project.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.project.pojo.entity.RelativePatientRelation;
import com.example.project.pojo.vo.AuthorizedPatientVo;

@Mapper
public interface RelativePatientRelationMapper {

    int insert(RelativePatientRelation row);

    int updateById(RelativePatientRelation row);

    int deleteById(@Param("id") Long id);

    RelativePatientRelation selectById(@Param("id") Long id);

    List<RelativePatientRelation> selectAll();

    List<AuthorizedPatientVo> selectAuthorizedPatientsByRelativeId(@Param("relativeId") Long relativeId);
}
