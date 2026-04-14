package com.example.project.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 登录成功时 data 字段：含 JWT */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginData {

    private String token;
    private String username;
}
