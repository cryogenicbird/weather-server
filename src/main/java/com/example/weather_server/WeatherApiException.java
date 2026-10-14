package com.example.weather_server;

public class WeatherApiException extends RuntimeException {
    public WeatherApiException(String message,Throwable cause) {
        super(message, cause);
    }
}