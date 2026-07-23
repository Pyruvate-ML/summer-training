SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS visit_record;
DROP TABLE IF EXISTS appointment;
DROP TABLE IF EXISTS audit_log;
DROP TABLE IF EXISTS patient_counselor;
DROP TABLE IF EXISTS patient;
DROP TABLE IF EXISTS doctor;
DROP TABLE IF EXISTS counselor;
DROP TABLE IF EXISTS user_profile;
DROP TABLE IF EXISTS site_message;
DROP TABLE IF EXISTS role_permission;
DROP TABLE IF EXISTS permission;
DROP TABLE IF EXISTS rbac_role;
DROP TABLE IF EXISTS app_user;
SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE app_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL UNIQUE,
  password_hash VARCHAR(100) NOT NULL,
  display_name VARCHAR(64) NOT NULL,
  role VARCHAR(32) NOT NULL COMMENT 'ADMIN/DOCTOR/PATIENT/COUNSELOR',
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_app_user_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE rbac_role (
  code VARCHAR(32) PRIMARY KEY,
  name VARCHAR(64) NOT NULL,
  description VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE permission (
  code VARCHAR(64) PRIMARY KEY,
  name VARCHAR(64) NOT NULL,
  module VARCHAR(64) NOT NULL,
  description VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE role_permission (
  role_code VARCHAR(32) NOT NULL,
  permission_code VARCHAR(64) NOT NULL,
  PRIMARY KEY (role_code, permission_code),
  CONSTRAINT fk_role_permission_role FOREIGN KEY (role_code) REFERENCES rbac_role(code),
  CONSTRAINT fk_role_permission_permission FOREIGN KEY (permission_code) REFERENCES permission(code)
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

CREATE TABLE counselor (
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

CREATE TABLE patient_counselor (
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
