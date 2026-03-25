package cn.lc.sunnyside.Auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 家属端 JWT 拦截器。
 *
 * 从 Authorization Bearer Token 中解析家属身份，并写入线程上下文。
 */
@Component
public class FamilyJwtInterceptor implements HandlerInterceptor {
    private final JWTVerifier jwtVerifier;

    /**
     * 当未配置密钥时，拦截器自动降级为透传模式（不做 JWT 校验）。
     */
    public FamilyJwtInterceptor(
            @Value("${app.auth.jwt.secret:}") String jwtSecret,
            @Value("${app.auth.jwt.issuer:sunnyside}") String jwtIssuer) {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            this.jwtVerifier = null;
            return;
        }
        this.jwtVerifier = JWT.require(Algorithm.HMAC256(jwtSecret))
                .withIssuer(jwtIssuer)
                .build();
    }

    /**
     * 请求进入时尝试解析 JWT，并将有效家属身份写入上下文。
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        FamilyLoginContext.clear();
        if (jwtVerifier == null) {
            return true;
        }
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return true;
        }
        String token = authorization.substring("Bearer ".length()).trim();
        if (token.isBlank()) {
            return true;
        }
        try {
            DecodedJWT jwt = jwtVerifier.verify(token);
            Long familyId = null;
            if (!jwt.getClaim("familyId").isNull()) {
                familyId = jwt.getClaim("familyId").asLong();
            }
            String familyPhone = null;
            if (!jwt.getClaim("familyPhone").isNull()) {
                familyPhone = jwt.getClaim("familyPhone").asString();
            }
            String familyUsername = null;
            if (!jwt.getClaim("familyUsername").isNull()) {
                familyUsername = jwt.getClaim("familyUsername").asString();
            }
            FamilyLoginContext context = new FamilyLoginContext(familyId, familyPhone, familyUsername);
            if (context.isValid()) {
                FamilyLoginContext.set(context);
            }
        } catch (JWTVerificationException ignored) {
            FamilyLoginContext.clear();
        }
        return true;
    }

    /**
     * 请求结束后无条件清理上下文，避免线程池复用造成身份泄露。
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
            Exception ex) {
        FamilyLoginContext.clear();
    }
}
