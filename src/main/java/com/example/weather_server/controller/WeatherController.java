package com.example.weather_server.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.example.weather_server.service.WeatherService;

@RestController
public class WeatherController {

    private final WeatherService weatherService;

    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @GetMapping("/weather/hourly/{lat}/{lon}")
    public Map<String, Object> weather(@PathVariable String lat, @PathVariable String lon) {
        return weatherService.getWeather(lat, lon);
    }
}
