package cn.lc.sunnyside.Service;

import cn.lc.sunnyside.POJO.DTO.FamilyAuthDTO;

/**
 * 家属认证服务抽象。
 */
public interface FamilyAuthService {
    /**
     * 生成登录验证码。
     *
     * @return 验证码响应数据
     */
    FamilyAuthDTO.FamilyCaptchaResponse createCaptcha();

    /**
     * 执行家属登录并签发令牌。
     *
     * @param request 登录请求
     * @return 登录响应
     */
    FamilyAuthDTO.FamilyLoginResponse login(FamilyAuthDTO.FamilyLoginRequest request);
}
