package com.example.weather_server;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class WeatherService {

    private static final String API_HOST = "https://kr5egpeb92.re.qweatherapi.com";
    private static final String LAT = "29.36";//重庆大学纬度
    private static final String LON = "106.18";//重庆大学经度
      // 上海经度
    private final RestClient client=RestClient.builder().build();

    /** 调用和风"城市实时天气"，返回裁剪后的精简 JSON */
    public Map<String, Object> getWeather(String lat, String lon) throws Exception {
        String token = getToken();
        Map<String, Object> resp = client.get()
            .uri(API_HOST + "/weather/v1/hourly/" + lat + "/" + lon + "?hours=240&localTime=true&lang=zh")
            .header("Authorization", "Bearer " + token)
            .retrieve()
            .body(Map.class);
        return resp;
    }
    public Map<String, Object> getWeather() throws Exception {
    return getWeather(LAT, LON);
}
    

    // ---- token 缓存：剩不到30秒才重签----
    private static String tokenCache;
    private static long tokenExpiresAt;

    private static synchronized String getToken() throws Exception {
        long now = System.currentTimeMillis() / 1000;
        if (tokenCache == null || now > tokenExpiresAt - 30) {
            tokenCache = JwtSigner.sign();
            tokenExpiresAt = now + 86300;
        }
        return tokenCache;
    }
}