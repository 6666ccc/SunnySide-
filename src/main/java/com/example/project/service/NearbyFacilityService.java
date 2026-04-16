package com.example.project.service;

import java.util.List;

import com.example.project.pojo.entity.NearbyFacility;

public interface NearbyFacilityService {

    List<NearbyFacility> listByDeptId(Long deptId);
}
