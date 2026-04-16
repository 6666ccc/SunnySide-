package com.example.project.service;

import java.util.List;

import com.example.project.common.Result;
import com.example.project.pojo.dto.BindPatientRequest;
import com.example.project.pojo.entity.RelativePatientRelation;
import com.example.project.pojo.vo.AuthorizedPatientVo;
import com.example.project.pojo.vo.PatientSearchVo;

public interface RelativePatientRelationService {

    int save(RelativePatientRelation row);

    int update(RelativePatientRelation row);

    int removeById(Long id);

    RelativePatientRelation getById(Long id);

    List<RelativePatientRelation> listAll();

    //根据亲属ID获取授权患者列表
    List<AuthorizedPatientVo> listAuthorizedPatients(Long relativeId);

    /** 按患者ID绑定患者（relativeId 须由 JWT 解析，勿由前端传入伪造） */
    Result<Void> bindByPatientId(Long relativeId, BindPatientRequest req);

    /** 家属解绑：校验关系行归属当前 relativeId */
    Result<Void> unbindByRelative(Long relativeId, Long relationId);

    /** 按患者ID搜索患者，返回脱敏信息 */
    List<PatientSearchVo> searchPatientById(Long patientId);
}
