package cn.lc.sunnyside.POJO.DTO;

import java.time.LocalDateTime;

public class FamilyAuthDTO {
    public record FamilyLoginRequest(String account, String password, String captchaId, String captchaCode) {
    }

    public record FamilyCaptchaResponse(String captchaId, String imageBase64, LocalDateTime expiresAt) {
    }

    public record FamilyLoginResponse(
            String token,
            String tokenType,
            LocalDateTime expiresAt,
            Long familyId,
            String familyPhone,
            String familyUsername,
            String familyName) {
    }
}
