package com.example.project.service;

import com.example.project.common.Result;
import com.example.project.pojo.dto.AuthRequest;
import com.example.project.pojo.dto.PatientLoginData;
import com.example.project.pojo.dto.PatientRegisterRequest;

public interface PatientAuthService {

    Result<PatientLoginData> login(AuthRequest request);

    Result<PatientLoginData> register(PatientRegisterRequest request);

    /** 将 JWT subject（username）解析为 patient.id */
    Long resolvePatientUserId(String jwtSubject);
}
