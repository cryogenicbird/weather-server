package com.example.weather_server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    private final WeatherService weatherService;

    
    public HelloController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }
    
    @GetMapping(value = "/hello", produces = MediaType.IMAGE_PNG_VALUE)
    public byte[] hello() throws IOException {
        return Files.readAllBytes(
            Path.of("C:/Users/81016/Pictures/Screenshots/屏幕截图(119).png"));
    }
    @GetMapping("/token")
    public String token() throws Exception {
    return JwtSigner.sign();
}    
    @GetMapping("/weather/hourly/{lat}/{lon}")
    public Map<String, Object> weather(@PathVariable String lat, @PathVariable String lon) throws Exception {
        return weatherService.getWeather(lat, lon);
    }
}
