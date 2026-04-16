package com.example.project.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.project.common.Result;
import com.example.project.pojo.dto.AuthRequest;
import com.example.project.pojo.dto.PatientLoginData;
import com.example.project.pojo.dto.PatientRegisterRequest;
import com.example.project.service.PatientAuthService;

@RestController
@RequestMapping("/patient")
public class PatientController {

    private final PatientAuthService patientAuthService;

    public PatientController(PatientAuthService patientAuthService) {
        this.patientAuthService = patientAuthService;
    }

    @PostMapping(value = "/login",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<PatientLoginData> login(@RequestBody AuthRequest body) {
        return patientAuthService.login(body);
    }

    @PostMapping(value = "/register",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<PatientLoginData> register(@RequestBody PatientRegisterRequest body) {
        return patientAuthService.register(body);
    }
}
