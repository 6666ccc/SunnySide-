package cn.lc.sunnyside.Controller;

import cn.lc.sunnyside.POJO.DTO.FamilyAuthDTO;
import cn.lc.sunnyside.Service.FamilyAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/family")
public class AuthController {
    private final FamilyAuthService familyAuthService;

    /**
     * 生成家属登录验证码。
     *
     * @return 验证码ID与Base64图片内容
     */
    @GetMapping("/captcha")
    public FamilyAuthDTO.FamilyCaptchaResponse captcha() {
        return familyAuthService.createCaptcha();
    }

    /**
     * 家属登录接口。
     *
     * @param request 登录请求，包含用户名、密码和验证码信息
     * @return 登录结果，包含令牌与基础身份信息
     */
    @PostMapping("/login")
    public FamilyAuthDTO.FamilyLoginResponse login(@RequestBody FamilyAuthDTO.FamilyLoginRequest request) {
        return familyAuthService.login(request);
    }

    /**
     * 处理请求参数与业务校验类异常。
     *
     * @param ex 业务抛出的参数异常
     * @return 标准错误响应
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @org.springframework.web.bind.annotation.ExceptionHandler(IllegalArgumentException.class)
    public ErrorResponse handleBadRequest(IllegalArgumentException ex) {
        return new ErrorResponse(ex.getMessage());
    }

    /**
     * 处理服务内部状态异常。
     *
     * @param ex 业务抛出的状态异常
     * @return 标准错误响应
     */
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @org.springframework.web.bind.annotation.ExceptionHandler(IllegalStateException.class)
    public ErrorResponse handleInternalError(IllegalStateException ex) {
        return new ErrorResponse(ex.getMessage());
    }

    public record ErrorResponse(String message) {
    }
}
