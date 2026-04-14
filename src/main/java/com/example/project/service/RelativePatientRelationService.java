package com.example.project.service;

import java.util.List;

import com.example.project.pojo.entity.RelativePatientRelation;
import com.example.project.pojo.vo.AuthorizedPatientVo;

public interface RelativePatientRelationService {

    int save(RelativePatientRelation row);

    int update(RelativePatientRelation row);

    int removeById(Long id);

    RelativePatientRelation getById(Long id);

    List<RelativePatientRelation> listAll();

    //根据亲属ID获取授权患者列表
    List<AuthorizedPatientVo> listAuthorizedPatients(Long relativeId);
}
