package cn.lc.sunnyside.Auth;

import java.util.Optional;

/**
 * 家属登录上下文持有器。
 *
 * 基于 ThreadLocal 在单次请求线程内传递登录态。
 */
public final class FamilyLoginContextHolder {
    private static final ThreadLocal<FamilyLoginContext> CONTEXT = new ThreadLocal<>();

    private FamilyLoginContextHolder() {
    }

    /**
     * 写入当前线程的登录上下文。
     */
    public static void set(FamilyLoginContext context) {
        CONTEXT.set(context);
    }

    /**
     * 读取当前线程的登录上下文。
     */
    public static Optional<FamilyLoginContext> get() {
        return Optional.ofNullable(CONTEXT.get());
    }

    /**
     * 清理当前线程的登录上下文，防止线程复用导致串号。
     */
    public static void clear() {
        CONTEXT.remove();
    }
}
