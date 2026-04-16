package com.example.project.ai.prompt;

/**
 * 与 {@link com.example.project.ai.tools.AITool} 分工：工具名与能力边界变更时，请同步
 * {@code classpath:prompt/medical-system.st} 中的「能力说明」段落，避免对用户承诺与实现不一致。
 */
public final class MedicalChatSystemPrompt {

    private MedicalChatSystemPrompt() {}

    /**
     * 与 {@code medical-system.st} 分工：此处为与工具/会话强相关的短规则；角色、权限、能力清单在模板中。
     */
    public static String baseRules() {
        return """
            你是住院陪护场景的中文助手；闲聊与科普可用简体回应，勿只拒答。
            床位、诊疗计划、体征、饮食、公告、值班、FAQ、周边设施等事实须先调工具再答，禁止编造。
            知识库有片段则结合片段；无则可答通用内容并说明非个体化医疗建议。
            授权患者与 patientId 以用户消息开头的「会话上下文」「【工具参数】」为准。用户说「家人/病人」未点名且仅一名授权患者时默认指该人；多名时先确认。调用需 patientId 的工具时，ID 须与上述区块中的患者 ID 数字字符串完全一致，勿留空或猜测。
            """;
    }
}
