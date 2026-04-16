package com.example.project.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.project.common.Result;
import com.example.project.mapper.PatientMapper;
import com.example.project.pojo.dto.AuthRequest;
import com.example.project.pojo.dto.PatientLoginData;
import com.example.project.pojo.dto.PatientRegisterRequest;
import com.example.project.pojo.entity.Patient;
import com.example.project.security.JwtUtil;
import com.example.project.service.PatientAuthService;

@Service
public class PatientAuthServiceImpl implements PatientAuthService {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    private PatientMapper patientMapper;

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    @Override
    public Result<PatientLoginData> login(AuthRequest request) {
        if (request == null) {
            return Result.fail("请求体不能为空");
        }
        String username = request.getUsername() == null ? null : request.getUsername().trim();
        String password = request.getPassword();
        if (isBlank(username) || isBlank(password)) {
            return Result.fail("用户名或密码不能为空");
        }
        String storedHash = patientMapper.selectPasswordByUsername(username);
        if (storedHash == null || !passwordEncoder.matches(password, storedHash)) {
            return Result.fail("用户名或密码错误");
        }
        Long patientId = patientMapper.selectIdByUsername(username);
        String token = JwtUtil.generateToken(username);
        return Result.ok("登录成功", new PatientLoginData(token, username, patientId));
    }

    @Override
    public Result<PatientLoginData> register(PatientRegisterRequest request) {
        if (request == null) {
            return Result.fail("请求体不能为空");
        }
        String admissionNo = request.getAdmissionNo() == null ? null : request.getAdmissionNo().trim();
        String username = request.getUsername() == null ? null : request.getUsername().trim();
        String password = request.getPassword();
        if (isBlank(admissionNo)) {
            return Result.fail("住院号不能为空");
        }
        if (isBlank(username) || isBlank(password)) {
            return Result.fail("用户名或密码不能为空");
        }

        Patient patient = patientMapper.selectByAdmissionNoForRegister(admissionNo);
        if (patient == null) {
            return Result.fail("住院号不存在，请核实");
        }
        if (!isBlank(patient.getUsername())) {
            return Result.fail("该患者已注册账号");
        }
        if (patientMapper.selectPasswordByUsername(username) != null) {
            return Result.fail("用户名已被占用");
        }

        String hash = passwordEncoder.encode(password);
        int rows = patientMapper.updateUsernameAndPassword(patient.getId(), username, hash);
        if (rows > 0) {
            String token = JwtUtil.generateToken(username);
            return Result.ok("注册成功", new PatientLoginData(token, username, patient.getId()));
        }
        return Result.fail("注册失败");
    }

    @Override
    public Long resolvePatientUserId(String jwtSubject) {
        if (jwtSubject == null || jwtSubject.isBlank()) {
            return null;
        }
        String s = jwtSubject.trim();
        Long id = patientMapper.selectIdByUsername(s);
        if (id != null) {
            return id;
        }
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
