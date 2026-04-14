package com.example.project.pojo.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class HospitalAnnouncement {
    private Long id;
    private Long deptId;
    private String title;
    private String content;
    private LocalDate publishDate;
    /** LOW, MEDIUM, HIGH */
    private String priority;
    private LocalDateTime createdAt;
}
