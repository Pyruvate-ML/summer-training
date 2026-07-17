package com.mindbridge.backend.controller;

import com.mindbridge.backend.entity.SystemUser;
import com.mindbridge.backend.entity.dto.AuthRequest;
import com.mindbridge.backend.interceptor.RequireRole;
import com.mindbridge.backend.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthController {

    @Autowired
    private AuthService authService;

    // 注册
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRequest.RegisterDTO dto) {
        try {
            authService.register(dto);
            return ResponseEntity.ok("注册成功");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 登录
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest.LoginDTO dto) {
        try {
            String token = authService.login(dto);
            Map<String, String> result = new HashMap<>();
            result.put("token", token);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    // 改密码，只能改自己的，登录了才能改
    @RequireRole("PATIENT")
    @PutMapping("/password")
    public ResponseEntity<?> changePassword(@RequestBody AuthRequest.PasswordDTO dto, HttpServletRequest request) {
        try {
            Long currentUserId = (Long) request.getAttribute("currentUserId");
            authService.changePassword(currentUserId, dto);
            return ResponseEntity.ok("密码已修改");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 改权限，这个必须是 ADMIN 才能改别人权限
    @RequireRole("ADMIN")
    @PutMapping("/users/{targetUserId}/role")
    public ResponseEntity<?> updateRole(@PathVariable Long targetUserId, @RequestParam String newRole) {
        try {
            authService.changeUserRole(targetUserId, newRole);
            return ResponseEntity.ok("已更新身份");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("更新失败");
        }
    }

    // 获取个人信息
    @RequireRole("PATIENT")
    @GetMapping("/users/me")
    public ResponseEntity<?> getCurrentUser(HttpServletRequest request) {
        try {
            Long currentUserId = (Long) request.getAttribute("currentUserId");
            SystemUser currentUser = authService.getCurrentUserInfo(currentUserId);

            return ResponseEntity.ok(currentUser);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 用户列表
    @RequireRole("ADMIN")
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        try {
            return ResponseEntity.ok(authService.getAllUsers());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 封号解封
    @RequireRole("ADMIN")
    @PutMapping("/users/{id}/status")
    public ResponseEntity<?> updateUserStatus(@PathVariable Long id, @RequestBody AuthRequest.StatusDTO dto) {
        try {
            authService.changeUserStatus(id, dto.getStatus());
            return ResponseEntity.ok("状态更新成功");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 账号提权与创建DOCTOR档案
    @RequireRole("ADMIN")
    @PostMapping("/users/{id}/upgrade-doctor")
    public ResponseEntity<?> upgradeToDoctor(@PathVariable Long id, @RequestBody AuthRequest.UpgradeDoctorDTO dto) {
        try {
            authService.upgradeToDoctor(id, dto);
            return ResponseEntity.ok("已提权并创建档案");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}