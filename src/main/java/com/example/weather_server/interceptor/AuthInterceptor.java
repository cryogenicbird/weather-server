package com.example.weather_server.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.example.weather_server.UserJwt;

/** 登录校验：所有需要鉴权的接口，先过这道门 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ") || !UserJwt.verify(auth.substring(7))) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":4003,\"message\":\"未登录或登录已过期\"}");
            return false;   // 不放行：请求到不了 Controller
        }
        return true;
    }
}
