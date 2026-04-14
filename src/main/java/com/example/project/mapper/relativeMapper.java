package com.example.project.mapper;

import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface relativeMapper {

    /** @return BCrypt 等密码哈希，用户不存在时为 null */
    String selectPasswordByUsername(@Param("username") String username);

    /** @return 受影响行数 */
    int insertUser(@Param("username") String username, @Param("passwordHash") String passwordHash);

    /** 不含 password 字段，便于展示 */
    Map<String, Object> selectAccountByUsername(@Param("username") String username);

    int updatePasswordHash(@Param("username") String username, @Param("passwordHash") String passwordHash);

    int deleteByUsername(@Param("username") String username);

    /** 用于根据登录名解析亲属主键 */
    Long selectIdByUsername(@Param("username") String username);
}
