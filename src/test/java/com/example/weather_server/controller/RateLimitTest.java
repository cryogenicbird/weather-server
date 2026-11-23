package com.example.weather_server.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.example.weather_server.UserMapper;
import com.example.weather_server.service.WeatherService;

/** 限流测试：60 次放行，第 61 次 429（Redis 用假实现，不需要真 Redis） */
@SpringBootTest
class RateLimitTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private UserMapper userMapper;

    @MockitoBean
    private WeatherService weatherService;

    // 假 Redis：opsForValue().increment(key) 每次返回递增的计数
    @MockitoBean
    private StringRedisTemplate redis;

    @MockitoBean
    private ValueOperations<String, String> valueOps;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        AtomicLong counter = new AtomicLong();
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(anyString())).thenAnswer(inv -> counter.incrementAndGet());
    }

    @Test
    void rateLimitReturns429() throws Exception {
        // 前 60 次放行到鉴权层（不带 token 所以 401），第 61 次被限流层直接 429
        // ——这恰好证明"限流在最外层"的注册顺序
        for (int i = 0; i < 60; i++) {
            mockMvc.perform(get("/weather/hourly/29.36/106.18"));
        }
        mockMvc.perform(get("/weather/hourly/29.36/106.18"))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.code").value(4004));
    }
}
