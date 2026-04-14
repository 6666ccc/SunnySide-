package com.example.project.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.project.ai.client.AIChat;
import com.example.project.service.relativeService;

@RestController
public class aiController {
    @Autowired
    private AIChat aiChat;

    @Autowired
    private relativeService relativeService;

    // 聊天接口
    @GetMapping("/chat")
    public String chat(@RequestAttribute("userId") String jwtSubject, @RequestParam("timeId") String timeId, @RequestParam("message") String message) {
        Long userId = relativeService.resolveRelativeUserId(jwtSubject);
        return aiChat.chat(userId, timeId, message);
    }
}
