package com.example.project.ai.prompt;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import com.example.project.pojo.vo.AuthorizedPatientVo;
import com.example.project.service.RelativePatientRelationService;

// 完善提示词: 给AI提供用户的信息(ID)以便于AI更好的回答用户的问题
@Slf4j
@Component
public class MedicalSystemPromptTemplate {

    /**
     * 系统提示：规则 + medical-system 静态指引（短）。用户提示：会话上下文（亲属/患者）+ 工具参数行 + 用户当前问题。
     */
    public record MedicalPromptParts(String systemPrompt, String sessionAndToolBlock) {
        /**
         * 组装 Spring AI 的 user 角色内容：会话与工具信息置于前，末尾为「【用户消息】」便于模型区分事实块与当前意图。
         */
        public String combinedUserMessage(String userMessage) {
            String body = userMessage == null ? "" : userMessage;
            if (sessionAndToolBlock == null || sessionAndToolBlock.isBlank()) {
                return "【用户消息】\n" + body;
            }
            return sessionAndToolBlock + "\n\n【用户消息】\n" + body;
        }
    }

    @Autowired
    private RelativePatientRelationService relativePatientRelationService;

    @Value("classpath:prompt/medical-system.st")
    private Resource templateResource;

    /**
     * userId：亲属用户在 relative_user 表中的主键（与 relative_patient_relation.relative_id 一致），
     * 用于查询其已授权关联的住院患者。
     */
    public String render(Long userId, String extraContext) {
        return renderParts(userId, extraContext).systemPrompt();
    }

    /**
     * 系统提示仅含规则与静态指引；动态 userId、患者列表、【工具参数】写入用户提示，减轻系统侧长度并便于模型聚焦工具入参。
     */
    public MedicalPromptParts renderParts(Long userId, String extraContext) {

        List<AuthorizedPatientVo> authorizedPatients =
                userId == null ? List.of() : relativePatientRelationService.listAuthorizedPatients(userId);

        String patientBlock = formatAuthorizedPatientsBlock(userId, authorizedPatients);
        log.info("与亲属绑定患者信息有: {}", patientBlock);

        PromptTemplate template = new PromptTemplate(templateResource);
        String system = template.render(Map.of("baseRules", MedicalChatSystemPrompt.baseRules()));

        String userIdLine = userId != null ? String.valueOf(userId) : "（未登录/未提供）";
        StringBuilder userContext = new StringBuilder();
        userContext.append("【会话上下文】\n");
        userContext.append("userId: ").append(userIdLine).append("\n");
        userContext.append("服务端当日(「今天」「今日」及 dutyDate/planDate 等日期参数以此为准): ")
                .append(LocalDate.now())
                .append("\n\n");
        userContext.append(patientBlock);
        if (extraContext != null && !extraContext.isBlank()) {
            userContext.append("\n\n").append(extraContext.trim());
        }
        userContext.append("\n\n").append(buildToolParameterPrefix(userId, authorizedPatients));

        return new MedicalPromptParts(system, userContext.toString());
    }

    private static String buildToolParameterPrefix(Long relativeUserId, List<AuthorizedPatientVo> patients) {
        if (relativeUserId == null) {
            return "【工具参数】当前未绑定亲属用户，调用需要 patientId 的工具时勿虚构 ID。";
        }
        if (patients == null || patients.isEmpty()) {
            return "【工具参数】当前账号暂无已授权住院患者，请勿对需要 patientId 的工具传入虚构 ID。";
        }
        if (patients.size() == 1) {
            AuthorizedPatientVo p = patients.get(0);
            String pid = formatPatientId(p.getPatientId());
            String name = nullToEmpty(p.getPatientName());
            return "【工具参数】用户所指「家人/患者」默认对应患者「" + name + "」。凡工具要求 patientId，必须传入字符串 \""
                    + pid + "\"（与系统提示中患者ID一致）。禁止留空或编造。";
        }
        StringBuilder sb = new StringBuilder("【工具参数】本账号关联多名患者，调用需 patientId 的工具时请按用户所指选用：");
        for (int i = 0; i < patients.size(); i++) {
            AuthorizedPatientVo p = patients.get(i);
            if (i > 0) {
                sb.append("；");
            }
            sb.append("患者ID ").append(formatPatientId(p.getPatientId()))
                    .append(" 对应 ").append(nullToEmpty(p.getPatientName()));
        }
        sb.append("。用户未点名时可先简要确认再调用工具。");
        return sb.toString();
    }

    /** 紧凑一行一名患者，减少系统提示长度，关键字段保留便于工具填 patientId */
    private static String formatAuthorizedPatientsBlock(Long relativeUserId, List<AuthorizedPatientVo> patients) {
        StringBuilder sb = new StringBuilder();
        sb.append("【关联住院患者】\n");
        if (relativeUserId == null) {
            sb.append("亲属用户ID未提供；勿推断床位/医嘱，仅通用科普。");
            return sb.toString();
        }
        sb.append("亲属ID(relative_user.id)=").append(relativeUserId).append("\n");
        if (patients == null || patients.isEmpty()) {
            sb.append("无已授权患者；勿虚构诊疗细节。");
            return sb.toString();
        }
        int i = 1;
        for (AuthorizedPatientVo p : patients) {
            sb.append(i++).append(") ")
                    .append("患者ID=").append(formatPatientId(p.getPatientId()))
                    .append(" 姓名=").append(nullToEmpty(p.getPatientName()))
                    .append(" 床=").append(nullToEmpty(p.getBedNumber()))
                    .append(" 科=").append(nullToEmpty(p.getDeptName()))
                    .append(" deptId=").append(formatPatientId(p.getDeptId()))
                    .append(" 住院号=").append(nullToEmpty(p.getAdmissionNo()))
                    .append(" 关系=").append(nullToEmpty(p.getRelationType()))
                    .append(" 法定代理=").append(formatLegalProxy(p.getIsLegalProxy()))
                    .append("\n");
        }
        return sb.toString().trim();
    }

    private static String nullToEmpty(String s) {
        if (s == null || s.isBlank()) {
            return "—";
        }
        return s;
    }

    private static String formatPatientId(Long patientId) {
        if (patientId == null) {
            return "—";
        }
        return String.valueOf(patientId);
    }

    private static String formatLegalProxy(Boolean isLegalProxy) {
        if (isLegalProxy == null) {
            return "未知";
        }
        if (Boolean.TRUE.equals(isLegalProxy)) {
            return "是";
        }
        return "否";
    }
}
