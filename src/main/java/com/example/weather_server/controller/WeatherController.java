package com.example.weather_server.controller;

import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.example.weather_server.service.WeatherService;

@Tag(name = "天气", description = "逐小时天气预报（需登录，Authorization 头带 Bearer token）")
@RestController
public class WeatherController {

    private final WeatherService weatherService;

    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @Operation(summary = "查询指定坐标的逐小时预报", description = "返回 240 小时裁剪数据（时间/天气现象/温度/降水）")
    @GetMapping("/weather/hourly/{lat}/{lon}")
    public Map<String, Object> weather(
            @Parameter(description = "纬度，最多两位小数，如 29.36") @PathVariable String lat,
            @Parameter(description = "经度，最多两位小数，如 106.18") @PathVariable String lon) {
        return weatherService.getWeather(lat, lon);
    }
}
