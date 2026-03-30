package cn.lc.sunnyside.Controller;

import cn.lc.sunnyside.POJO.DTO.RelativeAuthDTO;
import cn.lc.sunnyside.Service.RelativeAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author lc
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/relative")
public class AuthController {

    private final RelativeAuthService relativeAuthService;

    // 生成验证码接口
    @GetMapping("/captcha")
    public RelativeAuthDTO.CaptchaResponse captcha() {
        return relativeAuthService.createCaptcha();
    }

    // 登录接口
    @PostMapping("/login")
    public RelativeAuthDTO.LoginResponse login(@RequestBody RelativeAuthDTO.LoginRequest request) {
        return relativeAuthService.login(request);
    }

    // 异常处理
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @org.springframework.web.bind.annotation.ExceptionHandler(IllegalArgumentException.class)
    public ErrorResponse handleBadRequest(IllegalArgumentException ex) {
        return new ErrorResponse(ex.getMessage());
    }

    // 内部错误处理
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @org.springframework.web.bind.annotation.ExceptionHandler(IllegalStateException.class)
    public ErrorResponse handleInternalError(IllegalStateException ex) {
        return new ErrorResponse(ex.getMessage());
    }

    // 错误响应
    public record ErrorResponse(String message) {
    }
}
