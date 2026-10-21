package com.example.weather_server.controller;

import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.weather_server.service.AuthService;

@Tag(name = "认证", description = "注册与登录")
@RestController
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "注册新用户", description = "用户名唯一；密码至少 6 位，以 BCrypt 哈希入库")
    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody AuthRequest body) {
        authService.register(body.username(), body.password());
        return Map.of("code", 0, "message", "注册成功");
    }

    @Operation(summary = "登录", description = "成功后返回一年期 JWT，后续请求放入 Authorization 头")
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody AuthRequest body) {
        String token = authService.login(body.username(), body.password());
        return Map.of("code", 0, "message", "登录成功", "token", token);
    }
}
