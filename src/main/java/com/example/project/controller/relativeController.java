package com.example.project.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.project.common.Result;
import com.example.project.pojo.dto.AuthRequest;
import com.example.project.pojo.dto.LoginData;
import com.example.project.service.relativeService;

/**
 * 亲属端账户接口：登录、注册、查询、改密、注销。
 */
@RestController
@RequestMapping("/relative")
public class relativeController {

    private final relativeService relativeService;

    public relativeController(relativeService relativeService) {
        this.relativeService = relativeService;
    }

    @PostMapping(
            value = "/login",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<LoginData> login(@RequestBody AuthRequest body) {
        return relativeService.login(body);
    }

    @PostMapping(
            value = "/register",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<Void> register(@RequestBody AuthRequest body) {
        return relativeService.register(body);
    }

    @GetMapping("/getInfo")
    public String getInfo(@RequestParam String username) {
        return relativeService.getInfo(username);
    }

    @PostMapping("/updateInfo")
    public String updateInfo(
            @RequestParam String username,
            @RequestParam String oldPassword,
            @RequestParam String newPassword) {
        return relativeService.updateInfo(username, oldPassword, newPassword);
    }

    @PostMapping("/deleteInfo")
    public String deleteInfo(@RequestParam String username, @RequestParam String password) {
        return relativeService.deleteInfo(username, password);
    }
}
