package cn.lc.sunnyside.POJO.DTO;

import java.time.LocalDateTime;

public class FamilyAuthDTO {
    public record FamilyLoginRequest(String account, String password) {
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
