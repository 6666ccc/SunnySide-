package cn.lc.sunnyside.Workflow.common;

/**
 * 住院医疗工作流状态键：仅保留 Router + Tool Calling 模式所需的最小键集。
 */
public final class MedicalWorkflowKeys {

    public static final String QUERY = "query";
    public static final String PATIENT_ID = "patient_id";
    public static final String TOOL_CONTEXT = "tool_context";
    public static final String FINAL_REPLY = "final_reply";

    public static final String ROUTE_NEED_TOOLS = "need_tools";
    public static final String ROUTE_SKIP_TOOLS = "skip_tools";

    private MedicalWorkflowKeys() {
    }
}
