package cn.lc.sunnyside.Workflow.common;

/**
 * 住院医疗工作流状态键,用于在工作流中使用HashMap传递数据
 */
public final class MedicalWorkflowKeys {

    // 用户输入的问题/消息
    public static final String QUERY = "query";
    // 患者ID
    public static final String PATIENT_ID = "patient_id";
    // 工具上下文
    public static final String TOOL_CONTEXT = "tool_context";
    // 最终回复
    public static final String FINAL_REPLY = "final_reply";
    // 需要工具
    public static final String ROUTE_NEED_TOOLS = "need_tools";
    // 跳过工具
    public static final String ROUTE_SKIP_TOOLS = "skip_tools";

    // 私有构造函数，防止实例化
    private MedicalWorkflowKeys() {
    }
}
