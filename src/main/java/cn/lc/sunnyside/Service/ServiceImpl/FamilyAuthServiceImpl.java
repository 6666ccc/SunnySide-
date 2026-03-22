package cn.lc.sunnyside.Service.ServiceImpl;

import cn.lc.sunnyside.POJO.DO.FamilyUser;
import cn.lc.sunnyside.POJO.DTO.FamilyAuthDTO;
import cn.lc.sunnyside.Service.FamilyAuthService;
import cn.lc.sunnyside.mapper.FamilyUserMapper;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class FamilyAuthServiceImpl implements FamilyAuthService {
    private final FamilyUserMapper familyUserMapper;

    @Value("${app.auth.jwt.secret:}")
    private String jwtSecret;

    @Value("${app.auth.jwt.issuer:sunnyside}")
    private String jwtIssuer;

    @Value("${app.auth.jwt.expire-hours:168}")
    private Long jwtExpireHours;

    @Override
    public FamilyAuthDTO.FamilyLoginResponse login(FamilyAuthDTO.FamilyLoginRequest request) {
        if (request == null || isBlank(request.account()) || isBlank(request.password())) {
            throw new IllegalArgumentException("账号和密码不能为空。");
        }
        if (isBlank(jwtSecret)) {
            throw new IllegalStateException("服务端未配置JWT密钥。");
        }
        FamilyUser familyUser = findByAccount(request.account());
        if (familyUser == null || isBlank(familyUser.getPassword())) {
            throw new IllegalArgumentException("账号或密码错误。");
        }
        if (!familyUser.getPassword().equals(request.password().trim())) {
            throw new IllegalArgumentException("账号或密码错误。");
        }
        long ttlHours = normalizeTtlHours(jwtExpireHours);
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(ttlHours);
        String token = JWT.create()
                .withIssuer(jwtIssuer)
                .withClaim("familyId", familyUser.getId())
                .withClaim("familyPhone", familyUser.getPhone())
                .withClaim("familyUsername", familyUser.getUsername())
                .withExpiresAt(expiresAt.atZone(ZoneId.systemDefault()).toInstant())
                .sign(Algorithm.HMAC256(jwtSecret));
        return new FamilyAuthDTO.FamilyLoginResponse(
                token,
                "Bearer",
                expiresAt,
                familyUser.getId(),
                familyUser.getPhone(),
                familyUser.getUsername(),
                familyUser.getFullName());
    }

    private FamilyUser findByAccount(String account) {
        String normalized = account.trim();
        FamilyUser byPhone = familyUserMapper.selectByPhone(normalized);
        if (byPhone != null) {
            return byPhone;
        }
        return familyUserMapper.selectByUsername(normalized);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private long normalizeTtlHours(Long configuredHours) {
        long defaultHours = 24L;
        long maxHours = 168L;
        if (configuredHours == null || configuredHours <= 0) {
            return defaultHours;
        }
        return Math.min(configuredHours, maxHours);
    }
}
