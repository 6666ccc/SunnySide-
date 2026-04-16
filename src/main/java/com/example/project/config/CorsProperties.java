package com.example.project.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * 浏览器跨域（CORS）相关配置，由 {@code application.yml} 中 {@code app.cors} 绑定。
 */
@Data
@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {

    /**
     * 允许的 Origin 模式（Spring 支持 {@code http://localhost:*} 等形式）。
     * 生产环境请改为实际前端域名，勿长期使用过宽模式。
     */
    private List<String> allowedOriginPatterns = List.of("http://localhost:*", "http://127.0.0.1:*");

    /**
     * 是否允许携带 Cookie（仅使用 Bearer 头时可保持 false）。
     */
    private boolean allowCredentials = false;

    /**
     * 预检请求缓存时间（秒）。
     */
    private long maxAge = 3600;
}
