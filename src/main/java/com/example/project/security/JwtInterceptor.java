package com.example.project.security;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 拦截器，用于拦截请求，并进行鉴权
 */
@Component
public class JwtInterceptor implements HandlerInterceptor {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public boolean preHandle(HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws Exception {
        // 如果是OPTIONS请求，则直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 获取请求头中的Authorization字段
        String authHeader = request.getHeader(AUTH_HEADER);
        // 如果Authorization字段为空或者不以Bearer开头，则返回401状态码
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "未登录");
            return false;
        }

        // 获取Authorization字段中的token
        String token = authHeader.substring(BEARER_PREFIX.length());
        // 解析token
        try {
            String userId = JwtUtil.getUserId(token);
            // JWT subject 原样传递；是否为数字、何时转成 Long 由控制器/业务层决定
            request.setAttribute("userId", userId);
        } catch (Exception e) {
            // 如果解析token失败，则返回401状态码
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token无效");
            return false;
        }

        return true;
    }
}
