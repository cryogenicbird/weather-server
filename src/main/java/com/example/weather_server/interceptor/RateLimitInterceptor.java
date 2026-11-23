package com.example.weather_server.interceptor;

import java.time.Duration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 限流：同一 IP 每 60 秒最多 60 次。
 * 计数存 Redis（多实例共享、重启不丢）；Redis 故障时放行（fail-open）并记录日志。
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);

    private static final int MAX_PER_MINUTE = 60;
    private static final long WINDOW_SECONDS = 60;

    private final StringRedisTemplate redis;

    public RateLimitInterceptor(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String ip = request.getRemoteAddr();
        // 固定窗口：用"第几分钟"拼进 key，窗口过期靠 EXPIRE 清理
        long minuteSlice = System.currentTimeMillis() / (WINDOW_SECONDS * 1000);
        String key = "rl:" + ip + ":" + minuteSlice;

        Long count;
        try {
            // INCR 在 Redis 内单线程执行，天然原子；StringRedisTemplate 保证 key 是纯字符串
            count = redis.opsForValue().increment(key);
            if (count != null && count == 1) {
                // 只在第一次出现时设置过期，避免每次请求都刷新 TTL
                redis.expire(key, Duration.ofSeconds(WINDOW_SECONDS));
            }
        } catch (RedisConnectionFailureException e) {
            // fail-open：限流是保护措施，Redis 挂了不应该让整个服务不可用
            log.error("限流 Redis 不可用，本次请求放行", e);
            return true;
        }

        if (count != null && count > MAX_PER_MINUTE) {
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":4004,\"message\":\"请求过于频繁，请稍后再试\"}");
            return false;
        }
        return true;
    }
}
