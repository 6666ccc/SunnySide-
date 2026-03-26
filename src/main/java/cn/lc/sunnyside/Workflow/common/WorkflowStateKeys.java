package cn.lc.sunnyside.Workflow.common;

/**
 * 工作流状态键常量定义。
 * 统一不同节点间读写的状态字段名称，避免硬编码字符串分散。
 */
public final class WorkflowStateKeys {
    public static final String QUERY = "query";
    public static final String CONVERSATION_ID = "conversation_id";
    public static final String FAMILY_PHONE = "family_phone";
    public static final String FAMILY_ID = "family_id";
    public static final String INTENT = "intent";
    public static final String OPERATION = "operation";
    public static final String ELDER_ID = "elder_id";
    public static final String ELDER_NAME = "elder_name";
    public static final String TARGET_DATE = "target_date";
    public static final String BIZ_RESULT = "biz_result";
    public static final String ERROR_CODE = "error_code";
    public static final String ERROR_MESSAGE = "error_message";
    public static final String FINAL_REPLY = "final_reply";
    public static final String DOMAIN = "domain";

    /**
     * 禁止实例化常量类。
     */
    private WorkflowStateKeys() {
    }
}
