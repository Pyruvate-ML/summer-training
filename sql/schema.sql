-- 心桥心理咨询平台 MySQL 初始化脚本
-- 与 backend/src/main/resources/schema.sql、data.sql 保持一致。

CREATE DATABASE IF NOT EXISTS xinqiao_counseling
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE xinqiao_counseling;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS visit_record;
DROP TABLE IF EXISTS appointment;
DROP TABLE IF EXISTS audit_log;
DROP TABLE IF EXISTS patient;
DROP TABLE IF EXISTS doctor;
DROP TABLE IF EXISTS user_profile;
DROP TABLE IF EXISTS site_message;
DROP TABLE IF EXISTS app_user;
SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE app_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL UNIQUE,
  password_hash VARCHAR(100) NOT NULL,
  display_name VARCHAR(64) NOT NULL,
  role VARCHAR(32) NOT NULL COMMENT 'ADMIN/DOCTOR/PATIENT',
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_app_user_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_profile (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL UNIQUE,
  phone VARCHAR(32),
  email VARCHAR(128),
  department VARCHAR(128),
  office_location VARCHAR(128),
  emergency_contact VARCHAR(128),
  preference_note VARCHAR(255),
  bio VARCHAR(1000),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_profile_user FOREIGN KEY (user_id) REFERENCES app_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE doctor (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL UNIQUE,
  name VARCHAR(64) NOT NULL,
  title VARCHAR(128) NOT NULL,
  specialties VARCHAR(255) NOT NULL,
  schedule_note VARCHAR(255),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_doctor_user FOREIGN KEY (user_id) REFERENCES app_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE patient (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL UNIQUE,
  name VARCHAR(64) NOT NULL,
  student_no VARCHAR(64),
  college VARCHAR(128),
  grade VARCHAR(64),
  primary_topic VARCHAR(128) NOT NULL,
  assessment_level VARCHAR(32) NOT NULL,
  follow_plan VARCHAR(255),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_patient_level (assessment_level),
  CONSTRAINT fk_patient_user FOREIGN KEY (user_id) REFERENCES app_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE appointment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  patient_id BIGINT NOT NULL,
  doctor_id BIGINT NOT NULL,
  topic VARCHAR(128) NOT NULL,
  appointment_time DATETIME NOT NULL,
  status VARCHAR(32) NOT NULL,
  location VARCHAR(128) NOT NULL DEFAULT '心桥心理咨询室',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_appointment_patient (patient_id),
  INDEX idx_appointment_doctor (doctor_id),
  INDEX idx_appointment_status (status),
  CONSTRAINT fk_appointment_patient FOREIGN KEY (patient_id) REFERENCES patient(id),
  CONSTRAINT fk_appointment_doctor FOREIGN KEY (doctor_id) REFERENCES doctor(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE visit_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  patient_id BIGINT NOT NULL,
  doctor_id BIGINT NOT NULL,
  appointment_id BIGINT,
  visit_time DATETIME NOT NULL,
  diagnosis_summary VARCHAR(255) NOT NULL,
  treatment_note VARCHAR(1000) NOT NULL,
  next_plan VARCHAR(255),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_visit_patient (patient_id),
  INDEX idx_visit_doctor (doctor_id),
  CONSTRAINT fk_visit_patient FOREIGN KEY (patient_id) REFERENCES patient(id),
  CONSTRAINT fk_visit_doctor FOREIGN KEY (doctor_id) REFERENCES doctor(id),
  CONSTRAINT fk_visit_appointment FOREIGN KEY (appointment_id) REFERENCES appointment(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE site_message (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  sender_id BIGINT NOT NULL,
  receiver_id BIGINT NOT NULL,
  title VARCHAR(128) NOT NULL,
  content TEXT NOT NULL,
  is_read TINYINT(1) NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_message_receiver (receiver_id),
  INDEX idx_message_sender (sender_id),
  CONSTRAINT fk_message_sender FOREIGN KEY (sender_id) REFERENCES app_user(id),
  CONSTRAINT fk_message_receiver FOREIGN KEY (receiver_id) REFERENCES app_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE audit_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  operator_id BIGINT NOT NULL,
  operator_name VARCHAR(64) NOT NULL,
  operation_type VARCHAR(64) NOT NULL COMMENT 'LOGIN/UPDATE_PROFILE/VIEW_SENSITIVE/CREATE_APPOINTMENT',
  target_type VARCHAR(64) NOT NULL COMMENT 'user/user_profile/patient/appointment',
  target_id BIGINT,
  target_description VARCHAR(255),
  old_value TEXT,
  new_value TEXT,
  reason VARCHAR(500),
  ip_address VARCHAR(64),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_audit_operator (operator_id),
  INDEX idx_audit_type (operation_type),
  INDEX idx_audit_target (target_type, target_id),
  INDEX idx_audit_time (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO app_user(username, password_hash, display_name, role) VALUES
  ('admin', '$2a$10$9Gl09eKn08pXyQIXfK0IQ.Ot5nYlUCj8Cj/dasR5D4LVaqTrKG7DS', '系统管理员', 'ADMIN'),
  ('doctor_zhang', '$2a$10$UNZ8wbkyhMbDoCyLVGOSTODR5ee9hbkb1tTg7FdyqBF0GMlyhcM9e', '张明悦', 'DOCTOR'),
  ('doctor_lin', '$2a$10$UNZ8wbkyhMbDoCyLVGOSTODR5ee9hbkb1tTg7FdyqBF0GMlyhcM9e', '林思远', 'DOCTOR'),
  ('patient_chen', '$2a$10$niqUsCW6JD6sQ/juljPdHOgnploty5RR96.G8XcOV9ImaqZgZpZi.', '陈同学', 'PATIENT'),
  ('patient_li', '$2a$10$niqUsCW6JD6sQ/juljPdHOgnploty5RR96.G8XcOV9ImaqZgZpZi.', '李同学', 'PATIENT');

INSERT INTO user_profile(user_id, phone, email, department, office_location, emergency_contact, preference_note, bio) VALUES
  (1, '010-6600-2026', 'admin@xinqiao.demo', '心理中心运营组', '心桥心理咨询室 · 管理台', NULL, NULL, '负责平台排班、用户权限、数据概览和风险关注。'),
  (2, NULL, 'doctor_zhang@xinqiao.demo', '心理咨询组', '咨询室1 / 在线咨询', NULL, '周一至周五 09:00-17:00', '温和倾听，遵守伦理边界。'),
  (3, NULL, 'doctor_lin@xinqiao.demo', '心理咨询组', '咨询室2', NULL, '周二、周四 14:00-17:00', '关注学业困扰和人际关系支持。'),
  (4, NULL, NULL, '信息科学学院', NULL, '家长 / 13800000000', '面对面咨询，下午时段优先', '希望改善考试焦虑和睡眠节律。'),
  (5, NULL, NULL, '人文学院', NULL, '家长 / 13900000000', '团体辅导优先', '希望改善宿舍沟通和人际边界。');

INSERT INTO doctor(user_id, name, title, specialties, schedule_note) VALUES
  (2, '张明悦', '国家二级心理咨询师', '情绪调节,压力管理', '周一至周五 09:00-17:00'),
  (3, '林思远', '心理健康教育教师', '学业困扰,人际关系', '周二、周四 14:00-17:00');

INSERT INTO patient(user_id, name, student_no, college, grade, primary_topic, assessment_level, follow_plan) VALUES
  (4, '陈同学', '2026001', '信息科学学院', '2026级', '考试焦虑', '普通', '2 天后回访'),
  (5, '李同学', '2026002', '人文学院', '2026级', '宿舍关系', '关注', '本周团辅');

INSERT INTO appointment(patient_id, doctor_id, topic, appointment_time, status, location) VALUES
  (1, 1, '考试焦虑', '2026-07-20 10:30:00', '已排定', '咨询室1'),
  (2, 2, '宿舍关系', '2026-07-20 14:00:00', '静候确认', '咨询室2'),
  (1, 1, '复盘学习压力', '2026-07-17 16:00:00', '咨询已结束', '咨询室1');

INSERT INTO visit_record(patient_id, doctor_id, appointment_id, visit_time, diagnosis_summary, treatment_note, next_plan) VALUES
  (1, 1, 3, '2026-07-17 16:00:00', '阶段性考试焦虑，睡眠略受影响', '完成情绪识别练习，建议记录考前自动化想法。', '2 天后回访'),
  (2, 2, NULL, '2026-07-15 14:30:00', '宿舍沟通压力，人际边界不清', '讨论沟通边界和求助资源。', '本周团辅');

INSERT INTO site_message(sender_id, receiver_id, title, content, is_read) VALUES
  (1, 2, '测试邮件1', '测试能不能正常收邮件', 0),
  (2, 4, '？', '你人呢', 1),
  (4, 2, '。', '我懒得来了', 1);

INSERT INTO audit_log(operator_id, operator_name, operation_type, target_type, target_id, target_description, old_value, new_value, reason, ip_address, created_at) VALUES
  (1, '系统管理员', 'LOGIN', 'user', 1, '系统管理员登录系统', NULL, NULL, NULL, '127.0.0.1', '2026-07-21 08:30:00'),
  (1, '系统管理员', 'VIEW_SENSITIVE', 'patient', 1, '查看陈同学的咨询历史', NULL, NULL, '查看评估等级详情', '127.0.0.1', '2026-07-21 08:32:00'),
  (4, '陈同学', 'CREATE_APPOINTMENT', 'appointment', 2, '陈同学提交预约申请', NULL, '{"topic":"宿舍关系","status":"静候确认"}', '希望预约一次个体咨询', '127.0.0.1', '2026-07-21 09:10:00');
