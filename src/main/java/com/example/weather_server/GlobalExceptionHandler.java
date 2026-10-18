package com.example.weather_server;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;

/** 全局异常处理：统一翻译成 {"code", "message"} + 正确 HTTP 状态码 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 上游和风调用失败 → 502（我们是代理，上游出错就是 Bad Gateway） */
    @ExceptionHandler(WeatherApiException.class)
    public ResponseEntity<Map<String, Object>> handleUpstream(WeatherApiException e) {
        log.error("上游天气服务调用失败", e);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
            .body(Map.of("code", 5001, "message", e.getMessage()));
    }

    /** 兜底：其余一切 → 500 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleOther(Exception e) {
        log.error("未处理异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of("code", 5000, "message", "服务器内部错误"));
    }
    /** 业务校验失败（参数错/用户名重复）→ 400 */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of("code", 4001, "message", e.getMessage()));
}
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of("code", 4002, "message", "请求体格式错误（JSON 解析失败）"));
    }
}