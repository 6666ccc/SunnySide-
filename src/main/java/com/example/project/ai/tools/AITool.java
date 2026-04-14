package com.example.project.ai.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.project.service.InpatientAiDataService;

/**
 * 面向住院患者家属的 Spring AI 工具：描述与 {@code sql.sql} 中业务表一致，
 * 查库由 {@link InpatientAiDataService} 与 Mapper 层完成。
 */
@Component
public class AITool {

    /** 与 {@link com.example.project.ai.prompt.MedicalSystemPromptTemplate} 写入用户提示的「会话上下文」「【工具参数】」对齐。 */
    private static final String TOOL_DESC_PATIENT_ID_TAIL =
            " 参数 patientId 必须与用户提示中「会话上下文」所列患者ID及「【工具参数】」行数字字符串完全一致，勿留空或猜测。";

    @Autowired
    private InpatientAiDataService inpatientAiDataService;

    @Tool(description = """
            根据当前 HTTP 请求头中的 JWT（Authorization: Bearer，subject 为登录名）解析家属身份，查询亲属表 relative_user 与亲属_患者表 relative_patient_relation，
            返回家属主键 relativeUserId、姓名与手机号、已绑定患者 ID 列表 boundPatientIds 及床位/关系等明细 authorizedPatients。
            用于对话开始时自动确认「当前登录家属是谁」「可查询哪些 patientId」，便于后续 queryPatientBasicInfo 等工具直接填入患者 ID，无需用户重复输入。
            无参数调用即可；须与登录接口签发的令牌在同一请求上下文中使用。""")
    public String getCurrentRelativeContextFromJwt() {
        return inpatientAiDataService.getCurrentRelativeContextFromJwt();
    }

    @Tool(description = """
            列出当前登录家属账号已关联的全部住院患者及其关系（如配偶、子女、是否主要陪护/法定代理人）。
            用于多陪护人家庭在对话开始时确认要查询哪位病人，或核对系统侧授权范围。
            对应数据：亲属与患者关联表 relative_patient_relation、患者表 patient。
            无业务参数时调用即可。""")
    public String listAuthorizedPatientsForCurrentRelative() {
        return inpatientAiDataService.listAuthorizedPatientsForCurrentRelative();
    }

    @Tool(description = """
            查询指定患者在院的基础信息：姓名、性别、住院号/病案号、床号、所属科室、入院日期、出院或转科日期（若有）、在院状态（在院/已出院/已转科）。
            家属最常问「病人在哪一床」「哪个科」「什么时候入的院」等，优先使用本工具。
            对应数据：患者表 patient，必要时联表科室表 hospital_department 取科室名称与位置。
            参数 patientId：患者主键 ID（整数或字符串形式的数字）；若会话已绑定唯一病人可传入该 ID。"""
            + TOOL_DESC_PATIENT_ID_TAIL)
    public String queryPatientBasicInfo(String patientId) {
        return inpatientAiDataService.queryPatientBasicInfo(patientId);
    }

    @Tool(description = """
            查询患者所在科室或病区的公开信息：科室/病区名称、护士站或科室联系电话、病区位置（如楼层与区域）。
            用于家属问路、打电话到护士站、探视前确认地点。可传科室 ID，或与 queryPatientBasicInfo 联用已知 deptId。
            对应数据：hospital_department。""")
    public String queryDepartmentInfo(String departmentId) {
        return inpatientAiDataService.queryDepartmentInfo(departmentId);
    }

    @Tool(description = """
            按日期查询该患者的诊疗与护理计划（当日或预约日）：项目名称（如抽血、输液、CT、雾化等）、说明与注意事项、计划日期、预计开始/结束时间、地点、类型（手术/检查/输液/用药相关处置/用餐/护理/其他）、是否已执行。
            用于回答「今天有什么检查」「几点做 CT」「药/输液怎么安排的」——说明：库中无单独「药品字典」表，用药类医嘱常以计划项形式出现在本计划中（类别含用药、输液等）；具体药品名称与剂量以医院 HIS 或医护告知为准。
            对应数据：treatment_plan。
            参数 patientId：患者 ID；planDate：计划日期，格式建议 YYYY-MM-DD，缺省可表示今日。"""
            + TOOL_DESC_PATIENT_ID_TAIL)
    public String queryTreatmentPlanByDate(String patientId, String planDate) {
        return inpatientAiDataService.queryTreatmentPlanByDate(patientId, planDate);
    }

    @Tool(description = """
            查询患者在指定日期或最近一段时间的生命体征记录：测量日期时间、收缩压/舒张压、心率、血糖、体温、血氧、测量护士、异常或临床表现备注。
            用于家属了解「血压/体温怎么样」「有没有发烧」等，不可替代医生解读，可提示关注备注中的护理说明。
            对应数据：vital_signs。
            参数 patientId：患者 ID；recordDate：可选，单日查询用 YYYY-MM-DD；若查多日可由实现层扩展，此处占位。"""
            + TOOL_DESC_PATIENT_ID_TAIL)
    public String queryVitalSigns(String patientId, String recordDate) {
        return inpatientAiDataService.queryVitalSigns(patientId, recordDate);
    }

    @Tool(description = """
            查询指定科室在某一日期的医护值班信息：值班日期、医护姓名、职责角色（主任/主治/责任护士等）、班次（如白班/夜班）、病区紧急或公开联系电话。
            用于「今天谁管床」「责任护士电话多少」等（注意隐私与医院规定，仅返回业务允许对外展示的信息）。
            对应数据：medical_team_duty。
            参数 departmentId：科室 ID；dutyDate：日期 YYYY-MM-DD。""")
    public String queryMedicalTeamDuty(String departmentId, String dutyDate) {
        return inpatientAiDataService.queryMedicalTeamDuty(departmentId, dutyDate);
    }

    @Tool(description = """
            查询该患者在近期（默认未来约两周）需要接受的非用餐类诊疗与检查安排：手术、影像/化验等检查、输液、用药相关处置、护理等（对应 treatment_plan，已排除 MEAL）。
            用于家属问「要做哪些检查」「最近有什么治疗」「有什么项目安排」等；若只关心某一天可用 queryTreatmentPlanByDate。
            参数 patientId：患者主键 ID（字符串形式的数字）。"""
            + TOOL_DESC_PATIENT_ID_TAIL)
    public String queryTreatmentAndDetection(String patientId) {
        return inpatientAiDataService.queryTreatmentAndDetection(patientId);
    }

    @Tool(description = """
            查询患者医嘱相关的饮食安排：日期、餐次（早/午/晚/加餐）、饮食内容（如半流食、糖尿病餐等）、医嘱禁忌与营养说明（如低盐、禁食辛辣）。
            用于回答「能吃什么」「有什么忌口」，不等同于临床营养诊断，重大事项应遵医嘱。
            对应数据：dietary_advice。
            参数 patientId：患者 ID；mealDate：可选，YYYY-MM-DD，缺省可表示当日。"""
            + TOOL_DESC_PATIENT_ID_TAIL)
    public String queryDietaryAdvice(String patientId, String mealDate) {
        return inpatientAiDataService.queryDietaryAdvice(patientId, mealDate);
    }

    @Tool(description = """
            查询医院或科室发布的公告：标题、正文、发布日期、优先级；科室 ID 为空时表示全院公告。
            用于探视时间调整、住院须知、停水停电等公开通知，帮助家属提前安排行程。
            对应数据：hospital_announcement。
            参数 departmentId：可选，科室 ID；仅查全院公告时可传空或特定约定值；publishDateSince：可选，只拉取某日之后或近期的公告，格式 YYYY-MM-DD。""")
    public String queryHospitalAnnouncements(String departmentId, String publishDateSince) {
        return inpatientAiDataService.queryHospitalAnnouncements(departmentId, publishDateSince);
    }
}
