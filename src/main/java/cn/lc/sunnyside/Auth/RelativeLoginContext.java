package cn.lc.sunnyside.Auth;

import java.util.Optional;

/**F
 * 亲属登录上下文。
 *
 * @param relativeId 亲属ID
 * @param phone      亲属手机号
 * @param username   亲属用户名
 */
public record RelativeLoginContext(Long relativeId, String phone, String username) {

    private static final ThreadLocal<RelativeLoginContext> CONTEXT = new ThreadLocal<>();

    public boolean isValid() {
        return relativeId != null;
    }

    public static void set(RelativeLoginContext context) {
        CONTEXT.set(context);
    }

    public static Optional<RelativeLoginContext> get() {
        return Optional.ofNullable(CONTEXT.get());
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
