package com.example.weather_server.service;

import java.util.Map;

import com.example.weather_server.UserJwt;
import com.example.weather_server.UserMapper;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AuthService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /** 注册：查重 → BCrypt 哈希 → 入库（表里只有哈希，永远没有明文） */
    public void register(String username, String password) {
        if (username == null || password == null || password.length() < 6) {
            throw new IllegalArgumentException("用户名不能为空，密码至少 6 位");
        }
        if (userMapper.findByUsername(username) != null) {
            throw new IllegalArgumentException("用户名已存在");
        }
        userMapper.insert(username, encoder.encode(password));
    }
    public String login(String username, String password) {
    Map<String, Object> user = userMapper.findByUsername(username);
    // 统一返回"用户名或密码错误"——不区分哪个错，防止攻击者枚举哪些用户名存在
    if (user == null || !encoder.matches(password, (String) user.get("password_hash"))) {
        throw new IllegalArgumentException("用户名或密码错误");
    }
    return UserJwt.sign(username);
} 
}