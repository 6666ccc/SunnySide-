package com.example.project.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.project.mapper.RelativePatientRelationMapper;
import com.example.project.pojo.entity.RelativePatientRelation;
import com.example.project.pojo.vo.AuthorizedPatientVo;
import com.example.project.service.RelativePatientRelationService;

/* 家属患者关系服务实现类 */
@Service
public class RelativePatientRelationServiceImpl implements RelativePatientRelationService {

    @Autowired
    private RelativePatientRelationMapper relativePatientRelationMapper;

    /* 保存家属患者关系 */
    @Override
    public int save(RelativePatientRelation row) {
        return relativePatientRelationMapper.insert(row);
    }

    /* 更新家属患者关系 */
    @Override
    public int update(RelativePatientRelation row) {
        return relativePatientRelationMapper.updateById(row);
    }

    /* 删除家属患者关系 */
    @Override
    public int removeById(Long id) {
        return relativePatientRelationMapper.deleteById(id);
    }

    /* 根据ID获取家属患者关系 */
    @Override
    public RelativePatientRelation getById(Long id) {
        return relativePatientRelationMapper.selectById(id);
    }

    /* 获取所有家属患者关系 */
    @Override
    public List<RelativePatientRelation> listAll() {
        return relativePatientRelationMapper.selectAll();
    }

    /* 根据家属ID获取授权患者列表 */
    @Override
    public List<AuthorizedPatientVo> listAuthorizedPatients(Long relativeId) {
        return relativePatientRelationMapper.selectAuthorizedPatientsByRelativeId(relativeId);
    }
}
