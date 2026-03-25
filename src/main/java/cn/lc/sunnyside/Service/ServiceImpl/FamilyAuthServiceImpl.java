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

    /**
     * 创建并返回家属登录所需要的图形验证码
     *
     * @return 包含验证码ID、Base64格式图片和过期时间的响应对象
     */
    @Override
    public FamilyAuthDTO.FamilyCaptchaResponse createCaptcha() {
        // 读取并规范化验证码配置参数
        int width = normalizePositive(captchaWidth, 160);
        int height = normalizePositive(captchaHeight, 60);
        int codeCount = normalizePositive(captchaCodeCount, 4);
        int interfereCount = normalizePositive(captchaInterfereCount, 20);
        long ttlSeconds = normalizeCaptchaTtlSeconds(captchaTtlSeconds);
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(ttlSeconds);

        // 生成圆形干扰验证码
        CircleCaptcha captcha = CaptchaUtil.createCircleCaptcha(width, height, codeCount, interfereCount);
        String captchaId = UUID.randomUUID().toString().replace("-", "");
        String code = captcha.getCode();

        // 将验证码文本存入 Redis 缓存，用于后续校验
        try {
            stringRedisTemplate.opsForValue().set(buildCaptchaKey(captchaId), code.toUpperCase(),
                    Duration.ofSeconds(ttlSeconds));
        } catch (Exception ex) {
            throw new IllegalStateException("验证码服务暂不可用，请稍后重试。");
        }
        return new FamilyAuthDTO.FamilyCaptchaResponse(captchaId, toBase64(captcha), expiresAt);
    }

    /**
     * 家属登录逻辑，包括验证码校验和账号密码比对，并签发 JWT
     *
     * @param request 包含账号、密码、验证码等信息的登录请求
     * @return 包含签发的 JWT Token 及家属基本信息的响应对象
     */
    @Override
    public FamilyAuthDTO.FamilyLoginResponse login(FamilyAuthDTO.FamilyLoginRequest request) {
        // 校验基本参数是否为空
        if (request == null || isBlank(request.account()) || isBlank(request.password())
                || isBlank(request.captchaId()) || isBlank(request.captchaCode())) {
            throw new IllegalArgumentException("账号、密码和验证码不能为空。");
        }

        // 校验验证码的正确性
        verifyCaptcha(request.captchaId(), request.captchaCode());

        if (isBlank(jwtSecret)) {
            throw new IllegalStateException("服务端未配置JWT密钥。");
        }

        // 查询数据库确认家属账号是否存在
        FamilyUser familyUser = findByAccount(request.account());
        if (familyUser == null || isBlank(familyUser.getPassword())) {
            throw new IllegalArgumentException("账号或密码错误。");
        }

        // 校验密码（目前为明文比对，后续建议引入加密哈希校验）
        if (!familyUser.getPassword().equals(request.password().trim())) {
            throw new IllegalArgumentException("账号或密码错误。");
        }

        // 签发 JWT Token，携带家属的标识信息
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

    /**
     * 根据账号查询家属信息，优先按照手机号匹配，若无则按用户名匹配
     *
     * @param account 输入的账号（手机号或用户名）
     * @return 匹配的家属用户对象，不存在则返回 null
     */
    private FamilyUser findByAccount(String account) {
        String normalized = account.trim();
        FamilyUser byPhone = familyUserMapper.selectByPhone(normalized);
        if (byPhone != null) {
            return byPhone;
        }
        return familyUserMapper.selectByUsername(normalized);
    }

    /**
     * 判空辅助方法
     *
     * @param value 字符串内容
     * @return true 如果为空或纯空白字符，否则 false
     */
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * 构建验证码在 Redis 中的缓存 Key
     *
     * @param captchaId 验证码ID
     * @return Redis 缓存 Key
     */
    private String buildCaptchaKey(String captchaId) {
        String prefix = "auth:family:captcha:";
        if (!isBlank(captchaPrefix)) {
            prefix = captchaPrefix.trim();
        }
        return prefix + captchaId.trim();
    }

    /**
     * 校验图形验证码是否正确，并在校验后清理缓存（无论成功失败，防止复用）
     *
     * @param captchaId   验证码ID
     * @param captchaCode 用户输入的验证码文本
     */
    private void verifyCaptcha(String captchaId, String captchaCode) {
        String key = buildCaptchaKey(captchaId);
        String codeInRedis;
        try {
            // 从 Redis 中获取对应的验证码文本
            codeInRedis = stringRedisTemplate.opsForValue().get(key);
        } catch (Exception ex) {
            throw new IllegalStateException("验证码服务暂不可用，请稍后重试。");
        }

        if (isBlank(codeInRedis)) {
            throw new IllegalArgumentException("验证码不存在或已过期。");
        }

        String normalizedInput = captchaCode.trim().toUpperCase();

        // 验证码验证一次后立即删除，防止被暴力破解和复用
        try {
            stringRedisTemplate.delete(key);
        } catch (Exception ignored) {
        }

        if (!codeInRedis.equals(normalizedInput)) {
            throw new IllegalArgumentException("验证码错误。");
        }
    }

    /**
     * 将验证码图片对象转换为前端可直接使用的 Base64 数据 URL
     *
     * @param captcha 验证码对象
     * @return Data URL 格式的 Base64 字符串
     */
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
