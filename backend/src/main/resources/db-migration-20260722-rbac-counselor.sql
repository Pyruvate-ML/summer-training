CREATE TABLE IF NOT EXISTS rbac_role (
  code VARCHAR(32) PRIMARY KEY,
  name VARCHAR(64) NOT NULL,
  description VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS permission (
  code VARCHAR(64) PRIMARY KEY,
  name VARCHAR(64) NOT NULL,
  module VARCHAR(64) NOT NULL,
  description VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS role_permission (
  role_code VARCHAR(32) NOT NULL,
  permission_code VARCHAR(64) NOT NULL,
  PRIMARY KEY (role_code, permission_code),
  CONSTRAINT fk_role_permission_role FOREIGN KEY (role_code) REFERENCES rbac_role(code),
  CONSTRAINT fk_role_permission_permission FOREIGN KEY (permission_code) REFERENCES permission(code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS counselor (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL UNIQUE,
  name VARCHAR(64) NOT NULL,
  department VARCHAR(128),
  email VARCHAR(128),
  phone VARCHAR(32),
  office_location VARCHAR(128),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_counselor_user FOREIGN KEY (user_id) REFERENCES app_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS patient_counselor (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  patient_id BIGINT NOT NULL,
  counselor_id BIGINT NOT NULL,
  relation_type VARCHAR(64) NOT NULL DEFAULT '辅导员',
  active TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_patient_counselor (patient_id, counselor_id),
  INDEX idx_patient_counselor_patient (patient_id),
  INDEX idx_patient_counselor_counselor (counselor_id),
  CONSTRAINT fk_patient_counselor_patient FOREIGN KEY (patient_id) REFERENCES patient(id),
  CONSTRAINT fk_patient_counselor_counselor FOREIGN KEY (counselor_id) REFERENCES counselor(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO rbac_role(code, name, description) VALUES
  ('ADMIN', '管理员', '负责平台运营、账号、权限、统计和审计'),
  ('DOCTOR', '咨询师', '负责预约咨询、咨询记录和跟进计划'),
  ('COUNSELOR', '辅导员', '关注所带学生风险信号，接收预约联动通知'),
  ('PATIENT', '来访者', '提交预约，查看本人记录和跟进建议');

INSERT IGNORE INTO permission(code, name, module, description) VALUES
  ('dashboard:view', '查看工作台', 'dashboard', '查看角色首页'),
  ('appointments:read_all', '查看全部预约', 'appointment', '管理员查看所有预约'),
  ('appointments:read_related', '查看相关预约', 'appointment', '按角色查看相关预约'),
  ('appointments:create', '创建预约', 'appointment', '来访者或管理员创建预约'),
  ('patients:read_all', '查看全部来访者', 'patient', '管理员查看全部来访者管理字段'),
  ('patients:read_related', '查看相关来访者', 'patient', '咨询师和辅导员查看相关学生必要字段'),
  ('visit_records:read_related', '查看相关咨询记录', 'visit_record', '按角色查看相关咨询记录摘要'),
  ('visit_records:read_sensitive', '查看敏感详情', 'visit_record', '带原因查看咨询历史敏感详情'),
  ('messages:use', '使用站内信', 'message', '收发站内消息'),
  ('statistics:view', '查看数据统计', 'statistics', '管理员查看运营统计'),
  ('audit_logs:view', '查看审计日志', 'audit', '管理员查看审计日志'),
  ('rbac:view', '查看权限矩阵', 'rbac', '管理员查看角色权限矩阵'),
  ('users:manage', '管理用户', 'user', '管理员管理系统账号');

INSERT IGNORE INTO role_permission(role_code, permission_code) VALUES
  ('ADMIN', 'dashboard:view'), ('ADMIN', 'appointments:read_all'), ('ADMIN', 'appointments:create'),
  ('ADMIN', 'patients:read_all'), ('ADMIN', 'visit_records:read_related'), ('ADMIN', 'visit_records:read_sensitive'),
  ('ADMIN', 'messages:use'), ('ADMIN', 'statistics:view'), ('ADMIN', 'audit_logs:view'), ('ADMIN', 'rbac:view'), ('ADMIN', 'users:manage'),
  ('DOCTOR', 'dashboard:view'), ('DOCTOR', 'appointments:read_related'), ('DOCTOR', 'patients:read_related'),
  ('DOCTOR', 'visit_records:read_related'), ('DOCTOR', 'visit_records:read_sensitive'), ('DOCTOR', 'messages:use'),
  ('COUNSELOR', 'dashboard:view'), ('COUNSELOR', 'appointments:read_related'), ('COUNSELOR', 'patients:read_related'), ('COUNSELOR', 'messages:use'),
  ('PATIENT', 'dashboard:view'), ('PATIENT', 'appointments:read_related'), ('PATIENT', 'appointments:create'), ('PATIENT', 'visit_records:read_related'), ('PATIENT', 'messages:use');

INSERT IGNORE INTO app_user(username, password_hash, display_name, role)
VALUES ('counselor_wang', '$2a$10$UNZ8wbkyhMbDoCyLVGOSTODR5ee9hbkb1tTg7FdyqBF0GMlyhcM9e', '王老师', 'COUNSELOR');

INSERT IGNORE INTO counselor(user_id, name, department, email, phone, office_location)
SELECT id, '王老师', '信息科学学院', 'counselor_wang@xinqiao.demo', '13800001111', '学生工作办公室'
FROM app_user
WHERE username = 'counselor_wang';

INSERT IGNORE INTO patient_counselor(patient_id, counselor_id, relation_type, active)
SELECT p.id, c.id, '学院辅导员', 1
FROM patient p
JOIN app_user u ON u.id = p.user_id
JOIN counselor c
WHERE u.username = 'patient_chen'
  AND c.name = '王老师';
