package com.example.weather_server;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    private final WeatherService weatherService;

    public HelloController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @GetMapping("/weather/hourly/{lat}/{lon}")
    public Map<String, Object> weather(@PathVariable String lat, @PathVariable String lon) {
        return weatherService.getWeather(lat, lon);
    }
}
