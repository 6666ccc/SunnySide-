package com.example.project.pojo.entity;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class RelativePatientRelation {
    private Long id;
    private Long relativeId;
    private Long patientId;
    private String relationType;
    private Boolean isLegalProxy;
    private LocalDateTime createdAt;
}
