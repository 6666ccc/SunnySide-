package cn.lc.sunnyside.Workflow.common;

/**
 * 全项目 Graph 状态字段名常量。
 */
public final class WorkflowStateKeys {

    public static final String QUERY = "query";
    public static final String CONVERSATION_ID = "conversation_id";
    public static final String PATIENT_ID = "patient_id";
    public static final String TOOL_CONTEXT = "tool_context";
    public static final String FINAL_REPLY = "final_reply";
    public static final String RELATIVE_PHONE = "relative_phone";

    public static final String PROCESSED_QUERY = "processed_query";
    public static final String LLM_RESPONSE = "llm_response";

    public static final String CARE_ROUTE = "care_route";

    private WorkflowStateKeys() {
    }
}
