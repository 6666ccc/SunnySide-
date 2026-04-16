package com.example.project.pojo.entity;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class Faq {
    private Long id;
    private String category;
    private String question;
    private String answer;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}
