INSERT INTO app_user(username, display_name, role) VALUES
  ('admin', '系统管理员', 'ADMIN'),
  ('doctor_zhang', '张明悦', 'DOCTOR'),
  ('doctor_lin', '林思远', 'DOCTOR'),
  ('patient_chen', '陈同学', 'PATIENT'),
  ('patient_li', '李同学', 'PATIENT');

INSERT INTO doctor(user_id, name, title, specialties) VALUES
  (2, '张明悦', '国家二级心理咨询师', '情绪调节,压力管理'),
  (3, '林思远', '心理健康教育教师', '学业困扰,人际关系');

INSERT INTO patient(user_id, name, student_no, primary_topic, assessment_level, follow_plan) VALUES
  (4, '陈同学', '2026001', '考试焦虑', '普通', '2 天后回访'),
  (5, '李同学', '2026002', '宿舍关系', '关注', '本周团辅');

INSERT INTO appointment(patient_id, doctor_id, topic, appointment_time, status) VALUES
  (1, 1, '考试焦虑', '今天 10:30', '已排定'),
  (2, 2, '宿舍关系', '今天 14:00', '静候确认'),
  (1, 1, '复盘学习压力', '上周五 16:00', '咨询已结束');

INSERT INTO visit_record(patient_id, doctor_id, appointment_id, visit_time, diagnosis_summary, treatment_note, next_plan) VALUES
  (1, 1, 3, '上周五 16:00', '阶段性考试焦虑，睡眠略受影响', '完成情绪识别练习，建议记录考前自动化想法。', '2 天后回访'),
  (2, 2, NULL, '上周三 14:30', '宿舍沟通压力，人际边界不清', '讨论沟通边界和求助资源。', '本周团辅');
