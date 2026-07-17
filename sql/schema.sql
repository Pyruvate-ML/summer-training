-- 心桥心理咨询平台数据库初始化脚本
-- 建议数据库：MySQL 8.x

CREATE DATABASE IF NOT EXISTS mindbridge_counseling
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE mindbridge_counseling;

DROP TABLE IF EXISTS follow_up_record;
DROP TABLE IF EXISTS counseling_appointment;
DROP TABLE IF EXISTS client_profile;
DROP TABLE IF EXISTS counselor;
DROP TABLE IF EXISTS system_user;

CREATE TABLE system_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL UNIQUE COMMENT '登录账号',
  password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希',
  display_name VARCHAR(64) NOT NULL COMMENT '显示名称',
  role VARCHAR(32) NOT NULL DEFAULT 'COUNSELOR' COMMENT 'ADMIN/COUNSELOR/ASSISTANT',
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/DISABLED',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0
) COMMENT='系统用户表';

CREATE TABLE counselor (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(64) NOT NULL COMMENT '咨询师姓名',
  title VARCHAR(128) NOT NULL COMMENT '职称或身份',
  specialties VARCHAR(255) NOT NULL COMMENT '擅长方向，逗号分隔',
  current_load INT NOT NULL DEFAULT 0 COMMENT '当前负载百分比',
  user_id BIGINT NULL COMMENT '关联系统用户',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT fk_counselor_user FOREIGN KEY (user_id) REFERENCES system_user(id)
) COMMENT='咨询师表';

CREATE TABLE client_profile (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(64) NOT NULL COMMENT '来访者姓名或展示名',
  student_no VARCHAR(64) NULL COMMENT '学号，可为空',
  topic VARCHAR(128) NOT NULL COMMENT '主要咨询主题',
  assessment_level VARCHAR(32) NOT NULL DEFAULT '普通' COMMENT '普通/关注/重点',
  follow_plan VARCHAR(255) NULL COMMENT '跟进计划',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0
) COMMENT='来访者档案表';

CREATE TABLE counseling_appointment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  client_id BIGINT NOT NULL COMMENT '来访者ID',
  counselor_id BIGINT NOT NULL COMMENT '咨询师ID',
  topic VARCHAR(128) NOT NULL COMMENT '核心诉求',
  appointment_time VARCHAR(64) NOT NULL COMMENT '预约时间，前端原型阶段使用文本',
  status VARCHAR(32) NOT NULL DEFAULT '静候确认' COMMENT '静候确认/已排定/咨询已结束',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT fk_appointment_client FOREIGN KEY (client_id) REFERENCES client_profile(id),
  CONSTRAINT fk_appointment_counselor FOREIGN KEY (counselor_id) REFERENCES counselor(id)
) COMMENT='咨询预约表';

CREATE TABLE follow_up_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  client_id BIGINT NOT NULL COMMENT '来访者ID',
  counselor_id BIGINT NULL COMMENT '跟进咨询师ID',
  content VARCHAR(1000) NOT NULL COMMENT '跟进内容',
  next_plan VARCHAR(255) NULL COMMENT '下一步计划',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT fk_follow_client FOREIGN KEY (client_id) REFERENCES client_profile(id),
  CONSTRAINT fk_follow_counselor FOREIGN KEY (counselor_id) REFERENCES counselor(id)
) COMMENT='跟进记录表';

INSERT INTO system_user(username, password_hash, display_name, role)
VALUES
  ('admin', 'change-me', '系统管理员', 'ADMIN'),
  ('zhangmy', 'change-me', '张明悦', 'COUNSELOR');

INSERT INTO counselor(name, title, specialties, current_load, user_id)
VALUES
  ('张明悦', '国家二级心理咨询师', '情绪调节,压力管理', 68, 2),
  ('林思远', '心理健康教育教师', '学业困扰,人际关系', 52, NULL),
  ('周安宁', '危机干预专员', '风险评估,家庭沟通', 74, NULL),
  ('许知夏', '团体辅导老师', '自我探索,团体成长', 41, NULL),
  ('陈沐', '实习咨询师督导', '适应问题,生涯规划', 36, NULL),
  ('叶澄', '心理测评管理员', '量表测评,档案追踪', 47, NULL);

INSERT INTO client_profile(name, student_no, topic, assessment_level, follow_plan)
VALUES
  ('陈同学', NULL, '考试焦虑', '普通', '2 天后回访'),
  ('李同学', NULL, '人际困扰', '普通', '本周团辅'),
  ('王同学', NULL, '情绪低落', '重点', '今日复核'),
  ('赵同学', NULL, '自我探索', '普通', '预约中'),
  ('钱同学', NULL, '睡眠问题', '关注', '3 天后回访');

INSERT INTO counseling_appointment(client_id, counselor_id, topic, appointment_time, status)
VALUES
  (1, 1, '考试焦虑', '今天 10:30', '已排定'),
  (2, 2, '宿舍关系', '今天 14:00', '静候确认'),
  (3, 3, '情绪低落', '今天 16:20', '已排定'),
  (4, 4, '自我探索', '明天 09:30', '静候确认'),
  (5, 5, '生涯规划', '周五 15:00', '咨询已结束');
