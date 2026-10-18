package com.example.weather_server.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.example.weather_server.JwtSigner;
import com.example.weather_server.WeatherApiException;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class WeatherService {

    private static final String API_HOST = "https://kr5egpeb92.re.qweatherapi.com";
    private final RestClient client=RestClient.builder().build();

    /** 调用和风"小时天气预报"，返回裁剪后的精简 JSON */
    public Map<String, Object> getWeather(String lat, String lon){
        try{String token = getToken();
        Map<String, Object> resp = client.get()
            .uri(API_HOST + "/weather/v1/hourly/" + lat + "/" + lon + "?hours=240&localTime=true&lang=zh")
            .header("Authorization", "Bearer " + token)
            .retrieve()
            .body(Map.class);
        return trim(resp);
        } catch (RestClientException e) {
            throw new WeatherApiException("上游天气服务调用失败", e);
        }
    }

    /** 裁剪 */
    @SuppressWarnings("unchecked")
    private Map<String, Object> trim(Map<String, Object> resp) {
        List<Map<String, Object>> hours = new ArrayList<>();
        for (Object o : (List<Object>) resp.get("hours")) {
            Map<String, Object> h = (Map<String, Object>) o;
            Map<String, Object> condition = (Map<String, Object>) h.get("condition");
            Map<String, Object> temperature = (Map<String, Object>) h.get("temperature");
            Map<String, Object> precipitation = (Map<String, Object>) h.get("precipitation");
            Map<String, Object> intensity = (Map<String, Object>) precipitation.get("intensity");
            hours.add(Map.of(
                "forecastTime", h.get("forecastTime"),
                "condition", Map.of("text", condition.get("text"), "code", condition.get("code")),
                "temperature", Map.of("value", temperature.get("value")),
                "precipitation", Map.of(
                    "probability", precipitation.get("probability"),
                    "type", precipitation.get("type"),
                    "intensity", Map.of("value", intensity.get("value")))));
        }
        return Map.of("hours", hours);
    }    

    // ---- token 缓存：剩不到30秒才重签----
    private static String tokenCache;
    private static long tokenExpiresAt;

    private static synchronized String getToken() {
        long now = System.currentTimeMillis() / 1000;
        if (tokenCache == null || now > tokenExpiresAt - 30) {
            tokenCache = JwtSigner.sign();
            tokenExpiresAt = now + 86300;
        }
        return tokenCache;
    }
}