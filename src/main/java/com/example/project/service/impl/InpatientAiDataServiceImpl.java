package com.example.project.service.impl;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.example.project.mapper.relativeMapper;
import com.example.project.pojo.entity.DietaryAdvice;
import com.example.project.pojo.entity.HospitalAnnouncement;
import com.example.project.pojo.entity.HospitalDepartment;
import com.example.project.pojo.entity.MedicalTeamDuty;
import com.example.project.pojo.entity.TreatmentPlan;
import com.example.project.pojo.entity.VitalSigns;
import com.example.project.pojo.vo.AuthorizedPatientVo;
import com.example.project.pojo.vo.PatientBasicInfoVo;
import com.example.project.pojo.vo.RelativeSessionVo;
import com.example.project.security.JwtUtil;
import com.example.project.service.DietaryAdviceService;
import com.example.project.service.HospitalAnnouncementService;
import com.example.project.service.HospitalDepartmentService;
import com.example.project.service.InpatientAiDataService;
import com.example.project.service.MedicalTeamDutyService;
import com.example.project.service.PatientService;
import com.example.project.service.RelativePatientRelationService;
import com.example.project.service.TreatmentPlanService;
import com.example.project.service.VitalSignsService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 该类主要作用是将传统curd获得的数据转换为json格式，以便于AI模型使用,目的是方便AI进行数据查询和处理。
 */
@Service
public class InpatientAiDataServiceImpl implements InpatientAiDataService {

    /** 汇总「治疗/检查」类计划时，自今日起向后查询的天数（含起止日） */
    private static final int TREATMENT_DETECTION_LOOKAHEAD_DAYS = 14;

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Autowired
    private relativeMapper relativeMapper;

    @Autowired
    private RelativePatientRelationService relativePatientRelationService;

    @Autowired
    private PatientService patientService;

    @Autowired
    private HospitalDepartmentService hospitalDepartmentService;

    @Autowired
    private TreatmentPlanService treatmentPlanService;

    @Autowired
    private VitalSignsService vitalSignsService;

    @Autowired
    private MedicalTeamDutyService medicalTeamDutyService;

    @Autowired
    private DietaryAdviceService dietaryAdviceService;

    @Autowired
    private HospitalAnnouncementService hospitalAnnouncementService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 尝试从请求中解析出登录用户名
     * @return 登录用户名
     */
    private static String resolveJwtSubjectFromRequest() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (!(attrs instanceof ServletRequestAttributes sra)) {
            return null;
        }
        var req = sra.getRequest();
        Object u = req.getAttribute("userId");
        if (u != null && !u.toString().isBlank()) {
            return u.toString().trim();
        }
        String auth = req.getHeader(AUTH_HEADER);
        if (auth != null && auth.startsWith(BEARER_PREFIX)) {
            try {
                return JwtUtil.getUserId(auth.substring(BEARER_PREFIX.length()));
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }

    /**
     * 解析长整型ID
     * @param raw 原始字符串
     * @param fieldLabel 字段标签
     * @return 长整型ID
     */
    private static Long parseLongId(String raw, String fieldLabel) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException(fieldLabel + " 不能为空");
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldLabel + " 应为整数数字");
        }
    }

    private static LocalDate parseDateOrToday(String raw) {
        if (raw == null || raw.isBlank()) {
            return LocalDate.now();
        }
        return LocalDate.parse(raw.trim());
    }

    private static LocalDate parseDateNullable(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return LocalDate.parse(raw.trim());
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value != null ? value : Collections.emptyList());
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    /**
     * 获取当前登录家属的上下文信息
     * @return 当前登录家属的上下文信息
     */
    @Override
    public String getCurrentRelativeContextFromJwt() {
        String loginUsername = resolveJwtSubjectFromRequest();
        if (loginUsername == null || loginUsername.isEmpty()) {
            return "当前请求未携带有效 JWT（Authorization: Bearer）或不在 Web 请求上下文中，无法解析家属身份。";
        }
        Long relativeId = relativeMapper.selectIdByUsername(loginUsername);
        if (relativeId == null) {
            return "JWT 中的登录名未找到对应亲属记录。";
        }
        Map<String, Object> account = relativeMapper.selectAccountByUsername(loginUsername);
        List<AuthorizedPatientVo> patients =
                relativePatientRelationService.listAuthorizedPatients(relativeId);
        List<Long> patientIds = patients.stream().map(AuthorizedPatientVo::getPatientId).toList();

        RelativeSessionVo vo = new RelativeSessionVo();
        vo.setRelativeUserId(relativeId);
        vo.setLoginUsername(loginUsername);
        vo.setBoundPatientIds(patientIds);
        vo.setAuthorizedPatients(patients);
        if (account != null && !account.isEmpty()) {
            Object fn = account.get("full_name");
            Object ph = account.get("phone");
            vo.setFullName(fn != null ? fn.toString() : null);
            vo.setPhone(ph != null ? ph.toString() : null);
        }
        return toJson(vo);
    }

    /**
     * 获取当前登录家属的授权患者列表
     * @return 当前登录家属的授权患者列表
     */
    @Override
    public String listAuthorizedPatientsForCurrentRelative() {
        String username = resolveJwtSubjectFromRequest();
        if (username == null || username.isEmpty()) {
            return "当前请求未携带已登录家属身份（或不在 Web 请求上下文中），无法列出关联患者。";
        }
        Long relativeId = relativeMapper.selectIdByUsername(username);
        if (relativeId == null) {
            return "未找到登录账号对应的家属记录。";
        }
        List<AuthorizedPatientVo> rows = relativePatientRelationService.listAuthorizedPatients(relativeId);
        return toJson(rows);
    }

    /**
     * 查询患者基本信息
     * @param patientId 患者ID
     * @return 患者基本信息
     */
    @Override
    public String queryPatientBasicInfo(String patientId) {
        try {
            Long pid = parseLongId(patientId, "patientId");
            PatientBasicInfoVo vo = patientService.getBasicInfoWithDept(pid);
            if (vo == null) {
                return "未找到患者或科室信息。";
            }
            return toJson(vo);
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }

    /**
     * 查询科室信息
     * @param departmentId 科室ID
     * @return 科室信息
     */
    @Override
    public String queryDepartmentInfo(String departmentId) {
        try {
            Long did = parseLongId(departmentId, "departmentId");
            HospitalDepartment row = hospitalDepartmentService.getById(did);
            if (row == null) {
                return "未找到科室/病区信息。";
            }
            return toJson(row);
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }

    /**
     * 查询治疗计划
     * @param patientId 患者ID
     * @param planDate 治疗计划日期
     * @return 治疗计划
     */
    @Override
    public String queryTreatmentPlanByDate(String patientId, String planDate) {
        try {
            Long pid = parseLongId(patientId, "patientId");
            LocalDate date = parseDateOrToday(planDate);
            List<TreatmentPlan> rows = treatmentPlanService.listByPatientAndPlanDate(pid, date);
            return toJson(rows);
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        } catch (Exception e) {
            return "planDate 格式应为 YYYY-MM-DD";
        }
    }

    /**
     * 查询近期（默认未来两周）内非用餐类诊疗/检查计划，便于回答「要做哪些检查/治疗」。
     */
    @Override
    public String queryTreatmentAndDetection(String patientId) {
        try {
            Long pid = parseLongId(patientId, "patientId");
            LocalDate start = LocalDate.now();
            LocalDate end = start.plusDays(TREATMENT_DETECTION_LOOKAHEAD_DAYS - 1);
            // 查询近期（默认未来两周）内非用餐类诊疗/检查计划，便于回答「要做哪些检查/治疗」。
            List<TreatmentPlan> rows =
                    treatmentPlanService.listTreatmentAndExaminationByDateRange(pid, start, end);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("startDate", start.toString());
            body.put("endDate", end.toString());
            body.put("note", "以下为近两周内计划项（已排除 category=MEAL 的纯用餐安排）；含手术、检查、输液、用药、护理等，执行时间以病区为准。");
            body.put("plans", rows);
            return toJson(body);
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }

    /**
     * 查询生命体征
     * @param patientId 患者ID
     * @param recordDate 生命体征记录日期
     * @return 生命体征
     */
    @Override
    public String queryVitalSigns(String patientId, String recordDate) {
        try {
            Long pid = parseLongId(patientId, "patientId");
            LocalDate date = parseDateOrToday(recordDate);
            List<VitalSigns> rows = vitalSignsService.listByPatientAndRecordDate(pid, date);
            return toJson(rows);
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        } catch (Exception e) {
            return "recordDate 格式应为 YYYY-MM-DD";
        }
    }

    /**
     * 查询医疗团队职责
     * @param departmentId 科室ID
     * @param dutyDate 职责日期
     * @return 医疗团队职责
     */
    @Override
    public String queryMedicalTeamDuty(String departmentId, String dutyDate) {
        try {
            Long did = parseLongId(departmentId, "departmentId");
            LocalDate date = parseDateOrToday(dutyDate);
            List<MedicalTeamDuty> rows = medicalTeamDutyService.listByDeptAndDutyDate(did, date);
            return toJson(rows);
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        } catch (Exception e) {
            return "dutyDate 格式应为 YYYY-MM-DD";
        }
    }

    /**
     * 查询膳食建议
     * @param patientId 患者ID
     * @param mealDate 膳食建议日期
     * @return 膳食建议
     */
    @Override
    public String queryDietaryAdvice(String patientId, String mealDate) {
        try {
            Long pid = parseLongId(patientId, "patientId");
            LocalDate date = parseDateOrToday(mealDate);
            List<DietaryAdvice> rows = dietaryAdviceService.listByPatientAndMealDate(pid, date);
            return toJson(rows);
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        } catch (Exception e) {
            return "mealDate 格式应为 YYYY-MM-DD";
        }
    }

    /**
     * 查询医院公告
     * @param departmentId 科室ID
     * @param publishDateSince 公告发布日期
     * @return 医院公告
     */
    @Override
    public String queryHospitalAnnouncements(String departmentId, String publishDateSince) {
        try {
            Long deptId = null;
            if (departmentId != null && !departmentId.isBlank()) {
                deptId = Long.parseLong(departmentId.trim());
            }
            LocalDate since = parseDateNullable(publishDateSince);
            List<HospitalAnnouncement> rows = hospitalAnnouncementService.listAnnouncements(deptId, since);
            return toJson(rows);
        } catch (NumberFormatException e) {
            return "departmentId 应为整数数字";
        } catch (Exception e) {
            return "publishDateSince 格式应为 YYYY-MM-DD";
        }
    }
}
