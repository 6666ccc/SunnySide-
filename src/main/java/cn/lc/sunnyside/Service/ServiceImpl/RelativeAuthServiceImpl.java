package cn.lc.sunnyside.Service.ServiceImpl;

import cn.lc.sunnyside.POJO.DO.RelativeUser;
import cn.lc.sunnyside.POJO.DTO.RelativeAuthDTO;
import cn.lc.sunnyside.Service.RelativeAuthService;
import cn.lc.sunnyside.mapper.RelativeUserMapper;
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
public class RelativeAuthServiceImpl implements RelativeAuthService {

    private final RelativeUserMapper relativeUserMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Value("${app.auth.jwt.secret:}")
    private String jwtSecret;

    @Value("${app.auth.jwt.issuer:sunnyside}")
    private String jwtIssuer;

    @Value("${app.auth.jwt.expire-hours:168}")
    private Long jwtExpireHours;

    @Value("${app.auth.captcha.prefix:auth:relative:captcha:}")
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
    public RelativeAuthDTO.CaptchaResponse createCaptcha() {
        int width = normalizePositive(captchaWidth, 160);
        int height = normalizePositive(captchaHeight, 60);
        int codeCount = normalizePositive(captchaCodeCount, 4);
        int interfereCount = normalizePositive(captchaInterfereCount, 20);
        long ttlSeconds = normalizeTtlSeconds(captchaTtlSeconds);
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
        return new RelativeAuthDTO.CaptchaResponse(captchaId, toBase64(captcha), expiresAt);
    }

    @Override
    public RelativeAuthDTO.LoginResponse login(RelativeAuthDTO.LoginRequest request) {
        if (request == null || isBlank(request.account()) || isBlank(request.password())
                || isBlank(request.captchaId()) || isBlank(request.captchaCode())) {
            throw new IllegalArgumentException("账号、密码和验证码不能为空。");
        }

        verifyCaptcha(request.captchaId(), request.captchaCode());

        if (isBlank(jwtSecret)) {
            throw new IllegalStateException("服务端未配置JWT密钥。");
        }

        RelativeUser user = findByAccount(request.account());
        if (user == null || isBlank(user.getPassword())) {
            throw new IllegalArgumentException("账号或密码错误。");
        }
        if (!user.getPassword().equals(request.password().trim())) {
            throw new IllegalArgumentException("账号或密码错误。");
        }

        long ttlHours = normalizeTtlHours(jwtExpireHours);
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(ttlHours);
        String token = JWT.create()
                .withIssuer(jwtIssuer)
                .withClaim("relativeId", user.getId())
                .withClaim("relativePhone", user.getPhone())
                .withClaim("relativeUsername", user.getUsername())
                .withExpiresAt(expiresAt.atZone(ZoneId.systemDefault()).toInstant())
                .sign(Algorithm.HMAC256(jwtSecret));

        return new RelativeAuthDTO.LoginResponse(
                token, "Bearer", expiresAt,
                user.getId(), user.getPhone(), user.getUsername(), user.getFullName());
    }

    private RelativeUser findByAccount(String account) {
        String normalized = account.trim();
        RelativeUser byPhone = relativeUserMapper.selectByPhone(normalized);
        return byPhone != null ? byPhone : relativeUserMapper.selectByUsername(normalized);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String buildCaptchaKey(String captchaId) {
        String prefix = "auth:relative:captcha:";
        if (!isBlank(captchaPrefix)) {
            prefix = captchaPrefix.trim();
        }
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
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        captcha.write(os);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(os.toByteArray());
    }

    private long normalizeTtlSeconds(Long seconds) {
        if (seconds == null || seconds <= 0) return 120L;
        return Math.min(seconds, 600L);
    }

    private int normalizePositive(Integer value, int defaultValue) {
        return (value == null || value <= 0) ? defaultValue : value;
    }

    private long normalizeTtlHours(Long hours) {
        if (hours == null || hours <= 0) return 24L;
        return Math.min(hours, 168L);
    }
}
