package cn.lc.sunnyside.Service.medical;

/**
 * 医疗对话编排策略模式。
 */
public enum MedicalChatMode {
    /**
     * 默认：优先尝试 Agent Loop；失败后再尝试 Workflow；再不行回退到原 ChatClient 链路。
     */
    AUTO,
    /**
     * 强制走 Agent Loop（不做策略间 fallback）。
     */
    LOOP,
    /**
     * 强制走 Workflow（不做策略间 fallback）。
     */
    WORKFLOW
}

