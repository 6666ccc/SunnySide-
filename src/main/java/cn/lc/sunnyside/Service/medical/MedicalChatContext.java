package cn.lc.sunnyside.Service.medical;

/**
 * 医疗对话上下文（用于策略选择与执行）。
 *
 * threadId 用作两种场景的会话隔离：
 * - Agent Loop：RunnableConfig.threadId
 * - Workflow：BaseWorkflowExecutor 的 RunnableConfig.threadId（如果传入 conversationId/会话ID）
 */
public record MedicalChatContext(String query, String relativePhone, String threadId) {
}

