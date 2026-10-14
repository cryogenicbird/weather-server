package com.example.weather_server;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 全局异常处理：统一翻译成 {"code", "message"} + 正确 HTTP 状态码 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 上游和风调用失败 → 502（我们是代理，上游出错就是 Bad Gateway） */
    @ExceptionHandler(WeatherApiException.class)
    public ResponseEntity<Map<String, Object>> handleUpstream(WeatherApiException e) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
            .body(Map.of("code", 5001, "message", e.getMessage()));
    }

    /** 兜底：其余一切 → 500 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleOther(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of("code", 5000, "message", "服务器内部错误"));
    }
}