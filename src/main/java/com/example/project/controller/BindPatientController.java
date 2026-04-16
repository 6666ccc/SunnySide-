package com.example.project.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.project.common.Result;
import com.example.project.pojo.dto.BindPatientRequest;
import com.example.project.pojo.vo.AuthorizedPatientVo;
import com.example.project.pojo.vo.PatientSearchVo;
import com.example.project.service.RelativePatientRelationService;
import com.example.project.service.relativeService;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 按患者ID搜索/绑定患者、查看已绑定列表、解除绑定。
 * 身份从 JWT 解析，relativeId 不以请求参数传入。
 */
@RestController
@RequestMapping("/bindPatient")
public class BindPatientController {

    @Autowired
    private RelativePatientRelationService relativePatientRelationService;
    @Autowired
    private relativeService relativeService;

    private Long currentRelativeId(HttpServletRequest request) {
        Object raw = request.getAttribute("userId");
        if (raw == null) {
            return null;
        }
        String subject = raw.toString();
        return relativeService.resolveRelativeUserId(subject);
    }

    /**
     * 按患者ID搜索患者（返回脱敏信息，0 或 1 条）。
     */
    @GetMapping(value = "/searchPatient", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<List<PatientSearchVo>> searchPatient(
            HttpServletRequest request, @RequestParam("patientId") Long patientId) {
        if (currentRelativeId(request) == null) {
            return Result.fail(401, "未登录或账号无效");
        }
        return Result.ok(relativePatientRelationService.searchPatientById(patientId));
    }

    /**
     * 按患者ID绑定患者。
     */
    @PostMapping(value = "/bindByPatientId", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<Void> bindByPatientId(HttpServletRequest request, @RequestBody BindPatientRequest body) {
        Long relativeId = currentRelativeId(request);
        return relativePatientRelationService.bindByPatientId(relativeId, body);
    }

    /**
     * 当前登录家属已绑定的患者列表。
     */
    @GetMapping(value = "/myPatients", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<List<AuthorizedPatientVo>> myPatients(HttpServletRequest request) {
        Long relativeId = currentRelativeId(request);
        if (relativeId == null) {
            return Result.fail(401, "未登录或账号无效");
        }
        return Result.ok(relativePatientRelationService.listAuthorizedPatients(relativeId));
    }

    /**
     * 解除绑定（仅可删除属于当前家属的关系行）。
     */
    @DeleteMapping(value = "/unbind/{relationId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<Void> unbind(HttpServletRequest request, @PathVariable("relationId") Long relationId) {
        Long relativeId = currentRelativeId(request);
        return relativePatientRelationService.unbindByRelative(relativeId, relationId);
    }
}
