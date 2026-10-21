package com.example.weather_server.controller;

/** 注册/登录请求体：用 record 代替 Map，Swagger 才能生成有形状的文档 */
public record AuthRequest(String username, String password) {
}
