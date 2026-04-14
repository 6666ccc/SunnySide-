package com.example.project.pojo.dto;

import lombok.Data;

/** 登录 / 注册：JSON 请求体 */
@Data
public class AuthRequest {

    private String username;
    private String password;
}
