package com.example.project.service.impl;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import com.example.project.common.Result;
import com.example.project.mapper.PatientMapper;
import com.example.project.mapper.RelativePatientRelationMapper;
import com.example.project.pojo.dto.BindPatientRequest;
import com.example.project.pojo.entity.Patient;
import com.example.project.pojo.entity.RelativePatientRelation;
import com.example.project.pojo.vo.AuthorizedPatientVo;
import com.example.project.pojo.vo.PatientBasicInfoVo;
import com.example.project.pojo.vo.PatientSearchVo;
import com.example.project.service.RelativePatientRelationService;

/* 家属患者关系服务实现类 */
@Service
public class RelativePatientRelationServiceImpl implements RelativePatientRelationService {

    @Autowired
    private RelativePatientRelationMapper relativePatientRelationMapper;

    @Autowired
    private PatientMapper patientMapper;

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

    @Override
    public Result<Void> bindByPatientId(Long relativeId, BindPatientRequest req) {
        if (relativeId == null) {
            return Result.fail(401, "未登录或账号无效");
        }
        if (req == null) {
            return Result.fail("请求体不能为空");
        }
        Long patientId = req.getPatientId();
        String relationType = req.getRelationType() == null ? null : req.getRelationType().trim();
        if (patientId == null) {
            return Result.fail("患者ID不能为空");
        }
        if (relationType == null || relationType.isEmpty()) {
            return Result.fail("与患者关系不能为空");
        }
        Patient patient = patientMapper.selectById(patientId);
        if (patient == null) {
            return Result.fail("未找到该患者");
        }
        RelativePatientRelation existing =
                relativePatientRelationMapper.selectByRelativeAndPatient(relativeId, patientId);
        if (existing != null) {
            return Result.fail("已绑定该患者");
        }
        RelativePatientRelation row = new RelativePatientRelation();
        row.setRelativeId(relativeId);
        row.setPatientId(patientId);
        row.setRelationType(relationType);
        row.setIsLegalProxy(Boolean.TRUE.equals(req.getIsLegalProxy()));
        try {
            int n = relativePatientRelationMapper.insert(row);
            if (n > 0) {
                return Result.ok("绑定成功", null);
            }
            return Result.fail("绑定失败");
        } catch (DuplicateKeyException e) {
            return Result.fail("已绑定该患者");
        }
    }

    @Override
    public Result<Void> unbindByRelative(Long relativeId, Long relationId) {
        if (relativeId == null) {
            return Result.fail(401, "未登录或账号无效");
        }
        if (relationId == null) {
            return Result.fail("关系ID不能为空");
        }
        RelativePatientRelation row = relativePatientRelationMapper.selectById(relationId);
        if (row == null) {
            return Result.fail("绑定关系不存在");
        }
        if (!relativeId.equals(row.getRelativeId())) {
            return Result.fail(403, "无权解绑该关系");
        }
        int n = relativePatientRelationMapper.deleteById(relationId);
        if (n > 0) {
            return Result.ok("已解除绑定", null);
        }
        return Result.fail("解除绑定失败");
    }

    @Override
    public List<PatientSearchVo> searchPatientById(Long patientId) {
        if (patientId == null) {
            return Collections.emptyList();
        }
        Patient patient = patientMapper.selectById(patientId);
        if (patient == null) {
            return Collections.emptyList();
        }
        PatientBasicInfoVo basic = patientMapper.selectBasicInfoWithDept(patient.getId());
        PatientSearchVo vo = new PatientSearchVo();
        vo.setPatientId(patient.getId());
        vo.setPatientName(maskPatientName(patient.getPatientName()));
        vo.setBedNumber(patient.getBedNumber());
        vo.setAdmissionNo(maskAdmissionNo(patient.getAdmissionNo()));
        vo.setDeptName(basic != null ? basic.getDeptName() : null);
        return Collections.singletonList(vo);
    }

    /** 姓名脱敏：单字为 *，双字为首字+*，三字及以上为首+*+尾 */
    private static String maskPatientName(String name) {
        if (name == null || name.isBlank()) {
            return "*";
        }
        String n = name.trim();
        if (n.length() == 1) {
            return "*";
        }
        if (n.length() == 2) {
            return n.charAt(0) + "*";
        }
        return n.charAt(0) + "*" + n.charAt(n.length() - 1);
    }

    /** 住院号脱敏：保留末 4 位，前面用 * 代替 */
    private static String maskAdmissionNo(String admissionNo) {
        if (admissionNo == null || admissionNo.isBlank()) {
            return "";
        }
        String s = admissionNo.trim();
        if (s.length() <= 4) {
            return "****";
        }
        return "****" + s.substring(s.length() - 4);
    }
}
