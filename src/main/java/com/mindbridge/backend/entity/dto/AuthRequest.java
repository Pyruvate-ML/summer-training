package com.mindbridge.backend.entity.dto;

import lombok.Data;

public class AuthRequest {
    @Data
    public static class LoginDTO { // 登录
        private String username;
        private String password;
        private String loginRole; // 用户登录用的权限，比如 ADMIN 用了 DOCTOR 登录那就是 DOCTOR
    }
    @Data
    public static class RegisterDTO { // 注册
        private String username;
        private String password;
        private String displayName;
    }
    @Data
    public static class PasswordDTO { // 改密码
        private String oldPassword;
        private String newPassword;
    }
    @Data
    public static class StatusDTO { // 账户状态（封没被封）
        private String status;
    }
    @Data
    public static class UpgradeDoctorDTO { // DOCTOR 的一些个人信息
        private String title;
        private String specialties;
    }
}