package cn.lc.sunnyside.Auth;

/**
 * 家属登录上下文。
 *
 * @param familyId 家属ID
 * @param phone 家属手机号
 * @param username 家属用户名
 */
public record FamilyLoginContext(Long familyId, String phone, String username) {
    /**
     * 判断当前上下文是否可用于业务鉴权。
     */
    public boolean isValid() {
        return familyId != null && phone != null && !phone.isBlank();
    }
}
