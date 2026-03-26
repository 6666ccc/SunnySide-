package cn.lc.sunnyside.Config;

import cn.lc.sunnyside.Auth.FamilyJwtInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 全局跨域配置
 * 用于解决前端跨域访问后端接口的问题
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    private final FamilyJwtInterceptor familyJwtInterceptor;

    /**
     * 注册全局跨域策略。
     *
     * @param registry 跨域注册器
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * 添加 JWT 拦截器
     * 用于验证请求中的 JWT 令牌
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(familyJwtInterceptor).addPathPatterns("/**");
    }
}
