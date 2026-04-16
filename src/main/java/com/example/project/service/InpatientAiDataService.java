package com.example.project.service;

/**
 * 与 {@link com.example.project.ai.tools.AITool} 描述一致的读库能力（供工具层调用）。
 */
public interface InpatientAiDataService {

    /**
     * 从当前请求 JWT（subject 为患者端登录用户名）解析 {@code patient.id}；非患者账号或未登录时返回 {@code null}。
     */
    Long resolveCurrentPatientIdFromJwt();

    /**
     * 患者端：根据 JWT 解析当前登录患者，返回 patientId、登录名与 {@link com.example.project.pojo.vo.PatientBasicInfoVo} 等 JSON。
     */
    String getCurrentPatientContextFromJwt();

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

    String queryVitalSignsTrend(String patientId, String days);

    String queryTodayScheduleSummary(String patientId);

    String queryDischargeProgress(String patientId);

    String queryNearbyFacilities(String departmentId);

    String queryFrequentlyAskedQuestions(String category);

    // --- 患者本人账号（JWT 为患者用户名）：入参不含 patientId，由服务端绑定当前患者 ---

    String queryMyTreatmentPlanByDate(String planDate);

    String queryMyVitalSigns(String recordDate);

    String queryMyTreatmentAndDetection();

    String queryMyVitalSignsTrend(String days);

    String queryMyDietaryAdvice(String mealDate);

    String queryMyTodayScheduleSummary();

    String queryMyDischargeProgress();

    /** 当前患者所在科室/病区公开信息（依床号关联的 deptId）。 */
    String queryMyDepartmentInfo();

    /** 当前患者所在科室在指定日期的医护值班（dutyDate 空则按服务端当日）。 */
    String queryMyMedicalTeamDuty(String dutyDate);

    /** 本科室与全院公告；publishDateSince 可选。 */
    String queryMyHospitalAnnouncements(String publishDateSince);

    /** 当前患者所在科室周边便民设施。 */
    String queryMyNearbyFacilities();
}
