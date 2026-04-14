package com.example.project.service;

import com.example.project.common.Result;
import com.example.project.pojo.dto.AuthRequest;
import com.example.project.pojo.dto.LoginData;

public interface relativeService {

    /**
     * 将 JWT subject 转为 {@code relative_user.id}：当前登录实现 subject 为登录名，走按用户名查主键；
     * 若无匹配则再尝试将 subject 解析为数字主键（兼容 subject 直接存 id）。
     */
    Long resolveRelativeUserId(String jwtSubject);

    Result<LoginData> login(AuthRequest request);

    Result<Void> register(AuthRequest request);

    /** 查询账户公开信息（不含密码） */
    String getInfo(String username);

    /** 修改登录密码：校验原密码后更新为新密码 */
    String updateInfo(String username, String oldPassword, String newPassword);

    /** 校验密码后注销账户 */
    String deleteInfo(String username, String password);
}
