package com.example.project.pojo.entity;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class NearbyFacility {
    private Long id;
    private Long deptId;
    private String facilityName;
    /** CANTEEN, CONVENIENCE_STORE, PARKING, ATM, PHARMACY, OTHER */
    private String facilityType;
    private String location;
    private String distance;
    private LocalDateTime createdAt;
}
