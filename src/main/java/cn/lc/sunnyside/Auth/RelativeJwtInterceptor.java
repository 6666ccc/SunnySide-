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

@Component
public class RelativeJwtInterceptor implements HandlerInterceptor {

    private final JWTVerifier jwtVerifier;

    public RelativeJwtInterceptor(
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
        RelativeLoginContext.clear();
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
            Long relativeId = jwt.getClaim("relativeId").isNull() ? null : jwt.getClaim("relativeId").asLong();
            String relativePhone = jwt.getClaim("relativePhone").isNull() ? null : jwt.getClaim("relativePhone").asString();
            String relativeUsername = jwt.getClaim("relativeUsername").isNull() ? null : jwt.getClaim("relativeUsername").asString();
            RelativeLoginContext context = new RelativeLoginContext(relativeId, relativePhone, relativeUsername);
            if (context.isValid()) {
                RelativeLoginContext.set(context);
            }
        } catch (JWTVerificationException ignored) {
            RelativeLoginContext.clear();
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
            Exception ex) {
        RelativeLoginContext.clear();
    }
}
