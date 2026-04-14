package com.example.project.service;

/**
 * 与 {@link com.example.project.ai.tools.AITool} 描述一致的读库能力（供工具层调用）。
 */
public interface InpatientAiDataService {

    /**
     * 从当前请求的 JWT 解析家属身份，查询亲属信息与 {@code relative_patient_relation} 绑定的患者 ID。
     */
    String getCurrentRelativeContextFromJwt();

    String listAuthorizedPatientsForCurrentRelative();

    String queryPatientBasicInfo(String patientId);

    String queryDepartmentInfo(String departmentId);

    String queryTreatmentPlanByDate(String patientId, String planDate);

    String queryVitalSigns(String patientId, String recordDate);

    String queryMedicalTeamDuty(String departmentId, String dutyDate);

    String queryDietaryAdvice(String patientId, String mealDate);

    String queryHospitalAnnouncements(String departmentId, String publishDateSince);

    String queryTreatmentAndDetection(String patientId);
}
