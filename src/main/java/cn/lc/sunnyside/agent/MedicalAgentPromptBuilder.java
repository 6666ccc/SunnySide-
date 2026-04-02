package cn.lc.sunnyside.agent;

import cn.lc.sunnyside.Service.RelativeAccessService;

/**
 * 住院医疗 Agent 系统提示：与 {@link cn.lc.sunnyside.Workflow.MedicalRouterNode} 路由节点共用。
 */
public final class MedicalAgentPromptBuilder {

    public static final String ROUTER_SYSTEM = """
            你是住院医疗智能助手。当用户询问患者的生命体征（血压、心率、体温、血氧等）、诊疗计划（输液、手术、检查安排）、
            值班医疗团队（主治医生、责任护士）、饮食医嘱（餐食安排、禁忌）等问题时，必须调用提供的工具获取真实数据，不要编造。
            若系统提供了「默认患者ID」，调用住院业务工具时必须优先使用该患者ID，除非用户明确指定其他患者ID或能根据姓名在上下文中唯一确定另一名患者。
            如果用户问题中包含患者ID，也可直接使用；如果提到科室ID，也直接使用。
            可根据需要连续调用多个工具。若问题与住院医疗业务无关，可不调用工具。

            当你形成“最终回复”时，必须遵循回复规范：
            1）先给出关键结论（用不超过 2 句概括）。
            2）再列出关键依据：只使用工具调用返回的数据/RAG 信息，不要臆测或编造。
            3）最后给一句实用的健康建议或下一步行动建议。
            若工具数据不足以确认，请明确说明“暂时无法确认”，并给出可执行的下一步建议。""";

    private MedicalAgentPromptBuilder() {
    }

    /**
     * @param relativePhone    亲属手机号，可为 null（无登录上下文时仅返回基础系统提示）
     * @param defaultPatientId 默认患者 ID，可为 null
     */
    public static String build(String relativePhone, Long defaultPatientId, RelativeAccessService relativeAccessService) {
        if (relativePhone == null || relativePhone.isBlank()) {
            return ROUTER_SYSTEM;
        }
        String phone = relativePhone.trim();
        StringBuilder sb = new StringBuilder(ROUTER_SYSTEM);
        sb.append("\n\n【亲属与患者上下文】\n");
        sb.append(relativeAccessService.buildBoundPatientContext(phone));
        if (defaultPatientId != null) {
            sb.append("\n当前会话默认患者ID（工具入参 patientId 请使用该值，除非用户明确要求其他患者）：").append(defaultPatientId);
        }
        return sb.toString();
    }
}
