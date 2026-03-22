package cn.lc.sunnyside.Auth;

import java.util.Optional;

public final class FamilyLoginContextHolder {
    private static final ThreadLocal<FamilyLoginContext> CONTEXT = new ThreadLocal<>();

    private FamilyLoginContextHolder() {
    }

    public static void set(FamilyLoginContext context) {
        CONTEXT.set(context);
    }

    public static Optional<FamilyLoginContext> get() {
        return Optional.ofNullable(CONTEXT.get());
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
