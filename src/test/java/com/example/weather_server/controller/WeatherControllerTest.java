package com.example.weather_server.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.example.weather_server.UserJwt;
import com.example.weather_server.UserMapper;
import com.example.weather_server.service.WeatherService;

/** 天气接口鉴权测试：JWT 全链路（签发 → 携带 → 验签 → 放行）在测试里重演 */
@SpringBootTest
class WeatherControllerTest {

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
    void weatherWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/weather/hourly/29.36/106.18"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value(4003))
            .andExpect(jsonPath("$.message").value("未登录或登录已过期"));
    }

    @Test
    void weatherWithValidTokenIsOk() throws Exception {
        // 真签一个 token：Ed25519 签名 + 一年期 exp 全是真的
        String token = UserJwt.sign("test");

        // 假天气服务：不打真和风
        when(weatherService.getWeather(anyString(), anyString()))
            .thenReturn(Map.of("hours", List.of()));

        mockMvc.perform(get("/weather/hourly/29.36/106.18")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.hours").isArray());
    }
}
