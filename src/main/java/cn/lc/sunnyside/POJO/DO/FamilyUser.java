package cn.lc.sunnyside.POJO.DO;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FamilyUser {
    private Long id;
    private String username;
    private String password;
    private String fullName;
    private String phone;
    private String openId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
