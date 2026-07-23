INSERT INTO app_user(username, password_hash, display_name, role) VALUES
  ('admin', '$2a$10$9Gl09eKn08pXyQIXfK0IQ.Ot5nYlUCj8Cj/dasR5D4LVaqTrKG7DS', '系统管理员', 'ADMIN'),
  ('doctor_zhang', '$2a$10$UNZ8wbkyhMbDoCyLVGOSTODR5ee9hbkb1tTg7FdyqBF0GMlyhcM9e', '张明悦', 'DOCTOR'),
  ('doctor_lin', '$2a$10$UNZ8wbkyhMbDoCyLVGOSTODR5ee9hbkb1tTg7FdyqBF0GMlyhcM9e', '林思远', 'DOCTOR'),
  ('counselor_wang', '$2a$10$UNZ8wbkyhMbDoCyLVGOSTODR5ee9hbkb1tTg7FdyqBF0GMlyhcM9e', '王老师', 'COUNSELOR'),
  ('patient_chen', '$2a$10$niqUsCW6JD6sQ/juljPdHOgnploty5RR96.G8XcOV9ImaqZgZpZi.', '陈同学', 'PATIENT'),
  ('patient_li', '$2a$10$niqUsCW6JD6sQ/juljPdHOgnploty5RR96.G8XcOV9ImaqZgZpZi.', '李同学', 'PATIENT');

INSERT INTO rbac_role(code, name, description) VALUES
  ('ADMIN', '管理员', '负责平台运营、账号、权限、统计和审计'),
  ('DOCTOR', '咨询师', '负责预约咨询、咨询记录和跟进计划'),
  ('COUNSELOR', '辅导员', '关注所带学生风险信号，接收预约联动通知'),
  ('PATIENT', '来访者', '提交预约，查看本人记录和跟进建议');

INSERT INTO permission(code, name, module, description) VALUES
  ('dashboard:view', '查看工作台', 'dashboard', '查看角色首页'),
  ('appointments:read_all', '查看全部预约', 'appointment', '管理员查看所有预约'),
  ('appointments:read_related', '查看相关预约', 'appointment', '咨询师、辅导员、来访者查看相关预约'),
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

INSERT INTO role_permission(role_code, permission_code) VALUES
  ('ADMIN', 'dashboard:view'),
  ('ADMIN', 'appointments:read_all'),
  ('ADMIN', 'appointments:create'),
  ('ADMIN', 'patients:read_all'),
  ('ADMIN', 'visit_records:read_related'),
  ('ADMIN', 'visit_records:read_sensitive'),
  ('ADMIN', 'messages:use'),
  ('ADMIN', 'statistics:view'),
  ('ADMIN', 'audit_logs:view'),
  ('ADMIN', 'rbac:view'),
  ('ADMIN', 'users:manage'),
  ('DOCTOR', 'dashboard:view'),
  ('DOCTOR', 'appointments:read_related'),
  ('DOCTOR', 'patients:read_related'),
  ('DOCTOR', 'visit_records:read_related'),
  ('DOCTOR', 'visit_records:read_sensitive'),
  ('DOCTOR', 'messages:use'),
  ('COUNSELOR', 'dashboard:view'),
  ('COUNSELOR', 'appointments:read_related'),
  ('COUNSELOR', 'patients:read_related'),
  ('COUNSELOR', 'messages:use'),
  ('PATIENT', 'dashboard:view'),
  ('PATIENT', 'appointments:read_related'),
  ('PATIENT', 'appointments:create'),
  ('PATIENT', 'visit_records:read_related'),
  ('PATIENT', 'messages:use');

INSERT INTO user_profile(user_id, phone, email, department, office_location, emergency_contact, preference_note, bio) VALUES
  (1, '010-6600-2026', 'admin@xinqiao.demo', '心理中心运营组', '心桥心理咨询室 · 管理台', NULL, NULL, '负责平台排班、用户权限、数据概览和风险关注。'),
  (2, NULL, 'doctor_zhang@xinqiao.demo', '心理咨询组', '咨询室1 / 在线咨询', NULL, '周一至周五 09:00-17:00', '温和倾听，遵守伦理边界。'),
  (3, NULL, 'doctor_lin@xinqiao.demo', '心理咨询组', '咨询室2', NULL, '周二、周四 14:00-17:00', '关注学业困扰和人际关系支持。'),
  (4, '13800001111', 'counselor_wang@xinqiao.demo', '信息科学学院', '学生工作办公室', NULL, '重点关注高风险学生通知', '负责学生日常支持和危机联动。'),
  (5, NULL, NULL, '信息科学学院', NULL, '家长 / 13800000000', '面对面咨询，下午时段优先', '希望改善考试焦虑和睡眠节律。'),
  (6, NULL, NULL, '人文学院', NULL, '家长 / 13900000000', '团体辅导优先', '希望改善宿舍沟通和人际边界。');

INSERT INTO doctor(user_id, name, title, specialties, schedule_note) VALUES
  (2, '张明悦', '国家二级心理咨询师', '情绪调节,压力管理', '周一至周五 09:00-17:00'),
  (3, '林思远', '心理健康教育教师', '学业困扰,人际关系', '周二、周四 14:00-17:00');

INSERT INTO counselor(user_id, name, department, email, phone, office_location) VALUES
  (4, '王老师', '信息科学学院', 'counselor_wang@xinqiao.demo', '13800001111', '学生工作办公室');

INSERT INTO patient(user_id, name, student_no, college, grade, primary_topic, assessment_level, follow_plan) VALUES
  (5, '陈同学', '2026001', '信息科学学院', '2026级', '考试焦虑', '普通', '2 天后回访'),
  (6, '李同学', '2026002', '人文学院', '2026级', '宿舍关系', '关注', '本周团辅');

INSERT INTO patient_counselor(patient_id, counselor_id, relation_type, active) VALUES
  (1, 1, '学院辅导员', 1),
  (2, 1, '学院辅导员', 1);

INSERT INTO appointment(patient_id, doctor_id, topic, appointment_time, status, location) VALUES
  (1, 1, '考试焦虑', '2026-07-20 10:30:00', '已排定', '咨询室1'),
  (2, 2, '宿舍关系', '2026-07-20 14:00:00', '静候确认', '咨询室2'),
  (1, 1, '复盘学习压力', '2026-07-17 16:00:00', '咨询已结束', '咨询室1');

INSERT INTO visit_record(patient_id, doctor_id, appointment_id, visit_time, diagnosis_summary, treatment_note, next_plan) VALUES
  (1, 1, 3, '2026-07-17 16:00:00', '阶段性考试焦虑，睡眠略受影响', '完成情绪识别练习，建议记录考前自动化想法。', '2 天后回访'),
  (2, 2, NULL, '2026-07-15 14:30:00', '宿舍沟通压力，人际边界不清', '讨论沟通边界和求助资源。', '本周团辅');

INSERT INTO site_message(sender_id, receiver_id, title, content, is_read) VALUES
  (1, 2, '排班提醒', '请确认本周咨询排班。', 0),
  (2, 5, '咨询准备', '下次咨询前可以记录近期压力事件。', 1),
  (5, 2, '预约说明', '我想预约一次个体咨询。', 1);

INSERT INTO audit_log(operator_id, operator_name, operation_type, target_type, target_id, target_description, old_value, new_value, reason, ip_address, created_at) VALUES
  (1, '系统管理员', 'LOGIN', 'user', 1, '系统管理员登录系统', NULL, NULL, NULL, '127.0.0.1', '2026-07-21 08:30:00'),
  (1, '系统管理员', 'VIEW_SENSITIVE', 'patient', 1, '查看陈同学的咨询历史', NULL, NULL, '查看评估等级详情', '127.0.0.1', '2026-07-21 08:32:00'),
  (5, '陈同学', 'CREATE_APPOINTMENT', 'appointment', 2, '陈同学提交预约申请', NULL, '{"topic":"宿舍关系","status":"静候确认"}', '希望预约一次个体咨询', '127.0.0.1', '2026-07-21 09:10:00');
