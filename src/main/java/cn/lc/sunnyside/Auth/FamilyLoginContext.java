package cn.lc.sunnyside.Auth;

import java.util.Optional;

/**
 * 家属登录上下文。
 *
 * @param familyId 家属ID
 * @param phone    家属手机号
 * @param username 家属用户名
 */
public record FamilyLoginContext(Long familyId, String phone, String username) {

    private static final ThreadLocal<FamilyLoginContext> CONTEXT = new ThreadLocal<>();

    /**
     * 判断当前上下文是否可用于业务鉴权。
     */
    public boolean isValid() {
        return familyId != null;
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
