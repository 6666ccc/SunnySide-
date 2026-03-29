package cn.lc.sunnyside.POJO.DTO;

import java.time.LocalDateTime;

public class RelativeAuthDTO {

    public record LoginRequest(String account, String password, String captchaId, String captchaCode) {
    }

    public record CaptchaResponse(String captchaId, String imageBase64, LocalDateTime expiresAt) {
    }

    public record LoginResponse(
            String token,
            String tokenType,
            LocalDateTime expiresAt,
            Long relativeId,
            String relativePhone,
            String relativeUsername,
            String relativeName) {
    }
}
