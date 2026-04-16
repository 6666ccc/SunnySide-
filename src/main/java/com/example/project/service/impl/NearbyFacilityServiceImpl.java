package com.example.project.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.project.mapper.NearbyFacilityMapper;
import com.example.project.pojo.entity.NearbyFacility;
import com.example.project.service.NearbyFacilityService;

@Service
public class NearbyFacilityServiceImpl implements NearbyFacilityService {

    @Autowired
    private NearbyFacilityMapper nearbyFacilityMapper;

    @Override
    public List<NearbyFacility> listByDeptId(Long deptId) {
        return nearbyFacilityMapper.selectByDeptId(deptId);
    }
}
