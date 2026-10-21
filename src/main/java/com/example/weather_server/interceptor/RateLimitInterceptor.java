package com.example.weather_server.interceptor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** 限流：同一 IP 60 秒窗口内最多 60 次请求（固定窗口计数，内存版；量大后升级 Redis） */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final int MAX_PER_MINUTE = 60;
    private static final long WINDOW_MS = 60_000;

    // IP → [窗口开始时间, 窗口内计数]
    // ConcurrentHashMap：并发请求下 map 本身线程安全；
    // 数组的"检查+自增"不是原子操作，所以再套 synchronized 同步块
    private final Map<String, long[]> counters = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String ip = request.getRemoteAddr();
        long now = System.currentTimeMillis();

        long[] state = counters.computeIfAbsent(ip, k -> new long[]{now, 0});
        synchronized (state) {
            if (now - state[0] > WINDOW_MS) {
                state[0] = now;   // 窗口过期，重置
                state[1] = 0;
            }
            state[1]++;
            if (state[1] > MAX_PER_MINUTE) {
                response.setStatus(429);   // Too Many Requests
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":4004,\"message\":\"请求过于频繁，请稍后再试\"}");
                return false;
            }
        }
        return true;
    }

    /** 测试专用：清空计数器，避免测试间互相污染 */
    public void reset() {
        counters.clear();
    }
}
