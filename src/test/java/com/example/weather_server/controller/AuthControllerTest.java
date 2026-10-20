package com.example.weather_server.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.example.weather_server.UserMapper;
import com.example.weather_server.service.WeatherService;

/** 认证接口测试：UserMapper/WeatherService 用假货顶替——不碰真 MySQL、不打真和风、不依赖隧道 */
@SpringBootTest
class AuthControllerTest {

    // 用完整 Spring 上下文搭建 MockMvc：拦截器、异常处理器全都在岗（不走真网络）
    private MockMvc mockMvc;
    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private UserMapper userMapper;

    @MockitoBean
    private WeatherService weatherService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void registerSuccess() throws Exception {
        // 查重：库里没有同名用户 → null
        when(userMapper.findByUsername(anyString())).thenReturn(null);
        // 插入：返回 1（影响行数）
        when(userMapper.insert(anyString(), anyString())).thenReturn(1);

        mockMvc.perform(post("/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"test\",\"password\":\"123456\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.message").value("注册成功"));
    }
    @Test
    void registerFailDuplicate() throws Exception {
        // 查重：库里有同名用户 → 非 null
        when(userMapper.findByUsername(anyString()))
            .thenReturn(Map.of("username", "test", "password_hash", "$2a$10$xxxx"));

        mockMvc.perform(post("/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"test\",\"password\":\"123456\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(4001))
            .andExpect(jsonPath("$.message").value("用户名已存在"));
    }

    @Test
    void loginSuccess() throws Exception {
        // 用【真 BCrypt】给假用户造哈希——验密逻辑是真的在跑
        String realHash = new BCryptPasswordEncoder().encode("123456");
        when(userMapper.findByUsername(anyString()))
            .thenReturn(Map.of("username", "test", "password_hash", realHash));

        mockMvc.perform(post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"test\",\"password\":\"123456\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.token").exists());   // 真 JWT 已签发
    }

    @Test
    void loginFailWrongPassword() throws Exception {
        String realHash = new BCryptPasswordEncoder().encode("123456");
        when(userMapper.findByUsername(anyString()))
            .thenReturn(Map.of("username", "test", "password_hash", realHash));

        mockMvc.perform(post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"test\",\"password\":\"wrong66\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(4001))
            .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }
}