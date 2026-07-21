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

