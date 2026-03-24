package cn.lc.sunnyside.Service.ServiceImpl;

import cn.lc.sunnyside.POJO.DO.FamilyUser;
import cn.lc.sunnyside.POJO.DTO.FamilyAuthDTO;
import cn.lc.sunnyside.Service.FamilyAuthService;
import cn.lc.sunnyside.mapper.FamilyUserMapper;
import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.CircleCaptcha;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FamilyAuthServiceImpl implements FamilyAuthService {
    private final FamilyUserMapper familyUserMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Value("${app.auth.jwt.secret:}")
    private String jwtSecret;

    @Value("${app.auth.jwt.issuer:sunnyside}")
    private String jwtIssuer;

    @Value("${app.auth.jwt.expire-hours:168}")
    private Long jwtExpireHours;

    @Value("${app.auth.captcha.prefix:auth:family:captcha:}")
    private String captchaPrefix;

    @Value("${app.auth.captcha.ttl-seconds:120}")
    private Long captchaTtlSeconds;

    @Value("${app.auth.captcha.width:160}")
    private Integer captchaWidth;

    @Value("${app.auth.captcha.height:60}")
    private Integer captchaHeight;

    @Value("${app.auth.captcha.code-count:4}")
    private Integer captchaCodeCount;

    @Value("${app.auth.captcha.interfere-count:20}")
    private Integer captchaInterfereCount;

    @Override
    public FamilyAuthDTO.FamilyCaptchaResponse createCaptcha() {
        int width = normalizePositive(captchaWidth, 160);
        int height = normalizePositive(captchaHeight, 60);
        int codeCount = normalizePositive(captchaCodeCount, 4);
        int interfereCount = normalizePositive(captchaInterfereCount, 20);
        long ttlSeconds = normalizeCaptchaTtlSeconds(captchaTtlSeconds);
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(ttlSeconds);
        CircleCaptcha captcha = CaptchaUtil.createCircleCaptcha(width, height, codeCount, interfereCount);
        String captchaId = UUID.randomUUID().toString().replace("-", "");
        String code = captcha.getCode();
        try {
            stringRedisTemplate.opsForValue().set(buildCaptchaKey(captchaId), code.toUpperCase(),
                    Duration.ofSeconds(ttlSeconds));
        } catch (Exception ex) {
            throw new IllegalStateException("验证码服务暂不可用，请稍后重试。");
        }
        return new FamilyAuthDTO.FamilyCaptchaResponse(captchaId, toBase64(captcha), expiresAt);
    }

    @Override
    public FamilyAuthDTO.FamilyLoginResponse login(FamilyAuthDTO.FamilyLoginRequest request) {
        if (request == null || isBlank(request.account()) || isBlank(request.password())
                || isBlank(request.captchaId()) || isBlank(request.captchaCode())) {
            throw new IllegalArgumentException("账号、密码和验证码不能为空。");
        }
        verifyCaptcha(request.captchaId(), request.captchaCode());
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

    private String buildCaptchaKey(String captchaId) {
        String prefix = isBlank(captchaPrefix) ? "auth:family:captcha:" : captchaPrefix.trim();
        return prefix + captchaId.trim();
    }

    private void verifyCaptcha(String captchaId, String captchaCode) {
        String key = buildCaptchaKey(captchaId);
        String codeInRedis;
        try {
            codeInRedis = stringRedisTemplate.opsForValue().get(key);
        } catch (Exception ex) {
            throw new IllegalStateException("验证码服务暂不可用，请稍后重试。");
        }
        if (isBlank(codeInRedis)) {
            throw new IllegalArgumentException("验证码不存在或已过期。");
        }
        String normalizedInput = captchaCode.trim().toUpperCase();
        try {
            stringRedisTemplate.delete(key);
        } catch (Exception ignored) {
        }
        if (!codeInRedis.equals(normalizedInput)) {
            throw new IllegalArgumentException("验证码错误。");
        }
    }

    private String toBase64(CircleCaptcha captcha) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        captcha.write(outputStream);
        String rawBase64 = Base64.getEncoder().encodeToString(outputStream.toByteArray());
        return "data:image/png;base64," + rawBase64;
    }

    private long normalizeCaptchaTtlSeconds(Long configuredSeconds) {
        long defaultSeconds = 120L;
        long maxSeconds = 600L;
        if (configuredSeconds == null || configuredSeconds <= 0) {
            return defaultSeconds;
        }
        return Math.min(configuredSeconds, maxSeconds);
    }

    private int normalizePositive(Integer value, int defaultValue) {
        if (value == null || value <= 0) {
            return defaultValue;
        }
        return value;
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
