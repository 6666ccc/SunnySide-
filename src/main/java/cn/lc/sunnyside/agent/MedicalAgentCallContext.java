package cn.lc.sunnyside.agent;

/**
 * 单次同步 Agent 调用内的亲属/患者上下文（与当前请求线程绑定，在 Service 中 set/clear）。
 */
public record MedicalAgentCallContext(String relativePhone, Long defaultPatientId) {

    private static final ThreadLocal<MedicalAgentCallContext> HOLDER = new ThreadLocal<>();

    public static void set(MedicalAgentCallContext ctx) {
        HOLDER.set(ctx);
    }

    public static MedicalAgentCallContext current() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
