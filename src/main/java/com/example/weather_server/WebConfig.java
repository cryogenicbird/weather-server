package com.example.weather_server;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.example.weather_server.interceptor.AuthInterceptor;
import com.example.weather_server.interceptor.RateLimitInterceptor;

/** 拦截器注册：限流最外层（连登录接口一起保护，防爆破），鉴权内层 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;
    private final AuthInterceptor authInterceptor;

    public WebConfig(RateLimitInterceptor rateLimitInterceptor, AuthInterceptor authInterceptor) {
        this.rateLimitInterceptor = rateLimitInterceptor;
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
            .addPathPatterns("/**")
            .order(1);

        registry.addInterceptor(authInterceptor)
            .addPathPatterns("/**")
            // 白名单：注册/登录 + Swagger 文档页（文档不鉴权）
            .excludePathPatterns("/register", "/login",
                "/swagger-ui/**", "/v3/api-docs/**")
            .order(2);
    }
}
