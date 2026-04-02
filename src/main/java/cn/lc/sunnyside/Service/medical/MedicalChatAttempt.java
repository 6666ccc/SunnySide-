package cn.lc.sunnyside.Service.medical;

/**
 * 策略尝试结果：用于编排器做是否 fallback 的决策。
 *
 * @param reply 返回给客户端的文本（可能是错误说明）
 * @param success 是否视为“可用结果”
 */
public record MedicalChatAttempt(String reply, boolean success) {
}

