package com.mindbridge.backend.entity;

import lombok.Data;
import java.util.Date;

@Data // Lombok 牛逼
public class SystemUser {
    private Long id;
    private String username;
    private String passwordHash;
    private String displayName;
    private String role; // 三级权限，ADMIN,DOCTOR,PATIENT
    private String status;
    private Date createdAt;
    private Date updatedAt;
    private Integer isDeleted;
}