package com.mindbridge.backend.service;

import com.mindbridge.backend.entity.SystemUser;
import com.mindbridge.backend.entity.dto.AuthRequest;
import com.mindbridge.backend.mapper.DoctorMapper;
import com.mindbridge.backend.mapper.SystemUserMapper;
import com.mindbridge.backend.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuthService {

    @Autowired
    private SystemUserMapper userMapper;

    @Autowired
    private DoctorMapper doctorMapper;

    private String encryptPassword(String rawPassword) {
        return DigestUtils.md5DigestAsHex((rawPassword + "用中文当盐好像有点意思").getBytes());
    }

    public void register(AuthRequest.RegisterDTO dto) {
        if (userMapper.findByUsername(dto.getUsername()) != null) {
            throw new RuntimeException("用户名已被使用");
        }
        SystemUser user = new SystemUser();
        user.setUsername(dto.getUsername());
        user.setPasswordHash(encryptPassword(dto.getPassword()));
        user.setDisplayName(dto.getDisplayName());
        user.setRole("PATIENT"); // 注册默认是 PATIENT
        userMapper.insertUser(user);
    }

    public String login(AuthRequest.LoginDTO dto) {
        SystemUser user = userMapper.findByUsername(dto.getUsername());
        if (user == null || !user.getPasswordHash().equals(encryptPassword(dto.getPassword()))) {
            throw new RuntimeException("用户名或密码错误");
        }

        int userLevel = getRoleLevel(user.getRole());
        int loginLevel = getRoleLevel(dto.getLoginRole());
        if (loginLevel > userLevel) {
            throw new RuntimeException("权限不足");
        }

        return JwtUtil.generateToken(user.getId(), dto.getLoginRole());
    }

    public void changePassword(Long userId, AuthRequest.PasswordDTO dto) {
        SystemUser user = userMapper.findById(userId);
        if (!user.getPasswordHash().equals(encryptPassword(dto.getOldPassword()))) {
            throw new RuntimeException("旧密码错误");
        }
        userMapper.updatePassword(userId, encryptPassword(dto.getNewPassword()));
    }

    public void changeUserRole(Long targetUserId, String newRole) {
        userMapper.updateRole(targetUserId, newRole.toUpperCase());
    }

    public SystemUser getCurrentUserInfo(Long userId) {
        SystemUser user = userMapper.findById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        user.setPasswordHash(null);
        return user;
    }

    // 获取所有用户列表
    public List<SystemUser> getAllUsers() {
        List<SystemUser> users = userMapper.findAllUsers();
        // 遍历擦除密码哈希，防止泄露
        users.forEach(user -> user.setPasswordHash(null));
        return users;
    }

    // 封禁或者解封
    public void changeUserStatus(Long userId, String status) {
        userMapper.updateStatus(userId, status.toUpperCase());
    }

    @Transactional
    // 提权成医生，并且顺便绑定档案，这个比上面那个改 role 的更有用但是更专一
    public void upgradeToDoctor(Long userId, AuthRequest.UpgradeDoctorDTO dto) {
        SystemUser user = userMapper.findById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        userMapper.updateRole(userId, "DOCTOR");
        doctorMapper.insertDoctor(
                user.getDisplayName(),
                dto.getTitle(),
                dto.getSpecialties(),
                userId
        );
    }
    private int getRoleLevel(String role) {
        if (role == null) return 0;
        return switch (role.toUpperCase()) {
            case "ADMIN" -> 3;
            case "DOCTOR" -> 2;
            case "PATIENT" -> 1;
            default -> 0;
        };
    }
}