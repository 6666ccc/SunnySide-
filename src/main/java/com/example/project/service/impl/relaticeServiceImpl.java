package com.example.project.service.impl;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.project.common.Result;
import com.example.project.mapper.relativeMapper;
import com.example.project.pojo.dto.AuthRequest;
import com.example.project.pojo.dto.LoginData;
import com.example.project.security.JwtUtil;
import com.example.project.service.relativeService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class relaticeServiceImpl implements relativeService {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    private relativeMapper relativeMapper;

    @Autowired
    private ObjectMapper objectMapper;

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private String trimUsername(String username) {
        return username == null ? null : username.trim();
    }

    @Override
    public Long resolveRelativeUserId(String jwtSubject) {
        if (jwtSubject == null || jwtSubject.isBlank()) {
            return null;
        }
        String s = jwtSubject.trim();
        Long id = relativeMapper.selectIdByUsername(s);
        if (id != null) {
            return id;
        }
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /* 登录 */
    @Override
    public Result<LoginData> login(AuthRequest request) {
        if (request == null) {
            return Result.fail("请求体不能为空");
        }
        String u = trimUsername(request.getUsername());
        String password = request.getPassword();
        if (isBlank(u) || isBlank(password)) {
            return Result.fail("用户名或密码不能为空");
        }
        String storedHash = relativeMapper.selectPasswordByUsername(u);
        if (storedHash == null || !passwordEncoder.matches(password, storedHash)) {
            return Result.fail("用户名或密码错误");
        }
        String token = JwtUtil.generateToken(u);
        return Result.ok("登录成功", new LoginData(token, u));
    }

    /* 注册 */
    @Override
    public Result<Void> register(AuthRequest request) {
        if (request == null) {
            return Result.fail("请求体不能为空");
        }
        String u = trimUsername(request.getUsername());
        String password = request.getPassword();
        if (isBlank(u) || isBlank(password)) {
            return Result.fail("用户名或密码不能为空");
        }
        if (relativeMapper.selectPasswordByUsername(u) != null) {
            return Result.fail("用户名已存在");
        }
        String passwordHash = passwordEncoder.encode(password);
        int rows = relativeMapper.insertUser(u, passwordHash);
        if (rows > 0) {
            return Result.ok("注册成功", null);
        }
        return Result.fail("注册失败");
    }

    /* 获取用户信息 */
    @Override
    public String getInfo(String username) {
        String u = trimUsername(username);
        if (isBlank(u)) {
            return "用户名不能为空";
        }
        Map<String, Object> row = relativeMapper.selectAccountByUsername(u);
        if (row == null || row.isEmpty()) {
            return "用户不存在";
        }
        try {
            return objectMapper.writeValueAsString(row);
        } catch (JsonProcessingException e) {
            return "查询失败";
        }
    }

    /* 更新用户信息 */
    @Override
    public String updateInfo(String username, String oldPassword, String newPassword) {
        String u = trimUsername(username);
        if (isBlank(u) || isBlank(oldPassword) || isBlank(newPassword)) {
            return "用户名、原密码、新密码均不能为空";
        }
        if (oldPassword.equals(newPassword)) {
            return "新密码不能与原密码相同";
        }
        String storedHash = relativeMapper.selectPasswordByUsername(u);
        if (storedHash == null) {
            return "用户不存在";
        }
        if (!passwordEncoder.matches(oldPassword, storedHash)) {
            return "原密码错误";
        }
        String newHash = passwordEncoder.encode(newPassword);
        int rows = relativeMapper.updatePasswordHash(u, newHash);
        return rows > 0 ? "密码修改成功" : "密码修改失败";
    }

    /* 删除用户信息 */
    @Override
    public String deleteInfo(String username, String password) {
        String u = trimUsername(username);
        if (isBlank(u) || isBlank(password)) {
            return "用户名或密码不能为空";
        }
        String storedHash = relativeMapper.selectPasswordByUsername(u);
        if (storedHash == null || !passwordEncoder.matches(password, storedHash)) {
            return "用户名或密码错误";
        }
        int rows = relativeMapper.deleteByUsername(u);
        return rows > 0 ? "账号已注销" : "注销失败";
    }
}
