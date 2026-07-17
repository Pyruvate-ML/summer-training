package com.mindbridge.backend.interceptor;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.mindbridge.backend.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);
        // 如果没用 RequireRole 注解那就相当于零级权限，登录都不需要，可以直接看
        if (requireRole == null) {
            return true;
        }

        // 没登录不让看
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            response.setStatus(401);
            return false;
        }

        try {
            token = token.substring(7);
            DecodedJWT jwt = JwtUtil.verifyToken(token);
            String currentSessionRole = jwt.getClaim("role").asString();
            Long userId = jwt.getClaim("userId").asLong();
            request.setAttribute("currentUserId", userId);
            String requiredRole = requireRole.value();
            if (!hasPermission(currentSessionRole, requiredRole)) {
                response.setStatus(403); // 没权限
                return false;
            }

            return true;
        } catch (Exception e) {
            response.setStatus(401); // Token 过期了
            return false;
        }
    }
    private boolean hasPermission(String currentRole, String requiredRole) {
        int currentLevel = getRoleLevel(currentRole);
        int requiredLevel = getRoleLevel(requiredRole);
        return currentLevel >= requiredLevel;
    }
    // 管理员三级，医生二级，病人一级，没登录零级
    private int getRoleLevel(String role) {
        return switch (role.toUpperCase()) {
            case "ADMIN" -> 3;
            case "DOCTOR" -> 2;
            case "PATIENT" -> 1;
            default -> 0;
        };
    }
}