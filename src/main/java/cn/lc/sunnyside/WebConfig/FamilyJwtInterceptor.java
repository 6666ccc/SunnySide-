package cn.lc.sunnyside.WebConfig;

import cn.lc.sunnyside.Auth.FamilyLoginContext;
import cn.lc.sunnyside.Auth.FamilyLoginContextHolder;
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

@Component
public class FamilyJwtInterceptor implements HandlerInterceptor {
    private final JWTVerifier jwtVerifier;

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

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        FamilyLoginContextHolder.clear();
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
            Long familyId = jwt.getClaim("familyId").isNull() ? null : jwt.getClaim("familyId").asLong();
            String familyPhone = jwt.getClaim("familyPhone").isNull() ? null : jwt.getClaim("familyPhone").asString();
            String familyUsername = jwt.getClaim("familyUsername").isNull() ? null : jwt.getClaim("familyUsername").asString();
            FamilyLoginContext context = new FamilyLoginContext(familyId, familyPhone, familyUsername);
            if (context.isValid()) {
                FamilyLoginContextHolder.set(context);
            }
        } catch (JWTVerificationException ignored) {
            FamilyLoginContextHolder.clear();
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        FamilyLoginContextHolder.clear();
    }
}
