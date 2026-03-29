package cn.lc.sunnyside.Service;

import cn.lc.sunnyside.POJO.DTO.RelativeAuthDTO;

public interface RelativeAuthService {

    RelativeAuthDTO.CaptchaResponse createCaptcha();

    RelativeAuthDTO.LoginResponse login(RelativeAuthDTO.LoginRequest request);
}
