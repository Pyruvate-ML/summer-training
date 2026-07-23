package com.xinqiao.counseling;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLog;

    public ApiController(JdbcTemplate jdbc, PasswordEncoder passwordEncoder, AuditLogService auditLog) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
        this.auditLog = auditLog;
    }

    @PostMapping("/auth/login")
    Map<String, Object> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        if (request == null || isBlank(request.username()) || isBlank(request.password())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号或密码错误");
        }

        var users = jdbc.queryForList(
            """
            SELECT id, username, display_name, role, password_hash AS passwordHash
            FROM app_user
            WHERE username = ? AND enabled = 1
            """,
            request.username()
        );

        if (users.isEmpty() || !passwordEncoder.matches(request.password(), (String) users.get(0).get("passwordHash"))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号或密码错误");
        }

        var user = users.get(0);
        user.remove("passwordHash");
        auditLog.log(authForUsername(request.username()), "LOGIN", "user",
            ((Number) user.get("id")).longValue(),
            user.get("display_name") + "登录系统",
            null, null, null, clientIp(httpRequest));
        return sessionPayload(user);
    }

    @Transactional
    @PostMapping("/auth/register")
    Map<String, Object> register(@RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        if (request == null || isBlank(request.username()) || isBlank(request.password())
            || isBlank(request.displayName()) || isBlank(request.name())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "账号、密码、显示名称和姓名不能为空");
        }
        String role = "PATIENT";
        if ("PATIENT".equals(role) && isBlank(request.primaryTopic())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "来访者注册必须填写主要诉求");
        }
        if (!"PATIENT".equals(role)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "当前仅开放来访者自助注册");
        }
        Integer exists = jdbc.queryForObject("SELECT COUNT(*) FROM app_user WHERE username = ?", Integer.class, request.username().trim());
        if (exists != null && exists > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "账号已存在");
        }

        KeyHolder userKey = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var statement = connection.prepareStatement("""
                INSERT INTO app_user(username, password_hash, display_name, role)
                VALUES (?, ?, ?, ?)
                """, java.sql.Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, request.username().trim());
            statement.setString(2, passwordEncoder.encode(request.password()));
            statement.setString(3, request.displayName().trim());
            statement.setString(4, role);
            return statement;
        }, userKey);
        long userId = userKey.getKey().longValue();

        jdbc.update("""
            INSERT INTO patient(user_id, name, student_no, college, grade, primary_topic, assessment_level, follow_plan)
            VALUES (?, ?, ?, ?, ?, ?, '普通', '待首次咨询后生成跟进计划')
            """,
            userId,
            request.name().trim(),
            nullIfBlank(request.studentNo()),
            nullIfBlank(request.college()),
            nullIfBlank(request.grade()),
            request.primaryTopic().trim()
        );

        var user = jdbc.queryForMap(
            "SELECT id, username, display_name, role FROM app_user WHERE id = ?",
            userId
        );
        auditLog.log(authForUsername(request.username()), "REGISTER", "user", userId,
            request.displayName().trim() + "注册账号", null, null, "用户自助注册", clientIp(httpRequest));
        return sessionPayload(user);
    }

    @GetMapping("/me")
    Map<String, Object> me(Authentication auth) {
        return sessionPayload(currentUser(auth));
    }

    @GetMapping("/profile")
    Map<String, Object> profile(Authentication auth) {
        var user = currentUser(auth);
        return userProfile(user);
    }

    @PutMapping("/profile")
    Map<String, Object> updateProfile(Authentication auth, @RequestBody ProfileUpdateRequest request,
                                      HttpServletRequest httpRequest) {
        var user = currentUser(auth);
        if (request == null) {
            request = new ProfileUpdateRequest(null, null, null, null, null, null, null);
        }
        jdbc.update("""
            INSERT INTO user_profile(user_id, phone, email, department, office_location, emergency_contact, preference_note, bio)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
              phone = VALUES(phone),
              email = VALUES(email),
              department = VALUES(department),
              office_location = VALUES(office_location),
              emergency_contact = VALUES(emergency_contact),
              preference_note = VALUES(preference_note),
              bio = VALUES(bio)
            """,
            user.get("id"),
            nullIfBlank(request.phone()),
            nullIfBlank(request.email()),
            nullIfBlank(request.department()),
            nullIfBlank(request.officeLocation()),
            nullIfBlank(request.emergencyContact()),
            nullIfBlank(request.preferenceNote()),
            nullIfBlank(request.bio())
        );
        auditLog.log(auth, "UPDATE_PROFILE", "user_profile",
            ((Number) user.get("id")).longValue(),
            user.get("display_name") + "更新了个人资料",
            null, null, null, clientIp(httpRequest));
        return userProfile(user);
    }

    @GetMapping("/admin/dashboard")
    Map<String, Object> adminDashboard() {
        return Map.of(
            "users", count("app_user"),
            "doctors", count("doctor"),
            "patients", count("patient"),
            "appointments", count("appointment"),
            "visitRecords", count("visit_record")
        );
    }

    @GetMapping("/admin/statistics")
    Map<String, Object> adminStatisticsV2(@RequestParam(defaultValue = "month") String range) {
        String normalizedRange = normalizeRange(range);
        String appointmentDateFilter = rangeSql("a.created_at", normalizedRange);
        String visitDateFilter = rangeSql("vr.created_at", normalizedRange);
        String auditDateFilter = rangeSql("created_at", normalizedRange);

        return Map.of(
            "range", normalizedRange,
            "periodLabel", periodLabel(normalizedRange),
            "overview", Map.of(
                "users", count("app_user"),
                "doctors", count("doctor"),
                "patients", count("patient"),
                "appointments", count("appointment"),
                "visitRecords", count("visit_record"),
                "auditLogs", count("audit_log"),
                "periodAppointments", scalarCount("""
                    SELECT COUNT(*)
                    FROM appointment a
                    WHERE %s
                    """.formatted(appointmentDateFilter)),
                "periodVisitRecords", scalarCount("""
                    SELECT COUNT(*)
                    FROM visit_record vr
                    WHERE %s
                    """.formatted(visitDateFilter)),
                "activePatients", scalarCount("""
                    SELECT COUNT(DISTINCT patient_id)
                    FROM (
                      SELECT a.patient_id
                      FROM appointment a
                      WHERE %s
                      UNION
                      SELECT vr.patient_id
                      FROM visit_record vr
                      WHERE %s
                    ) x
                    """.formatted(appointmentDateFilter, visitDateFilter)),
                "activeDoctors", scalarCount("""
                    SELECT COUNT(DISTINCT doctor_id)
                    FROM (
                      SELECT a.doctor_id
                      FROM appointment a
                      WHERE %s
                      UNION
                      SELECT vr.doctor_id
                      FROM visit_record vr
                      WHERE %s
                    ) x
                    """.formatted(appointmentDateFilter, visitDateFilter))
            ),
            "appointmentStatusDistribution", jdbc.queryForList("""
                SELECT status, COUNT(*) AS total
                FROM appointment
                WHERE %s
                GROUP BY status
                ORDER BY total DESC, status
                """.formatted(rangeSql("created_at", normalizedRange))),
            "doctorWorkload", jdbc.queryForList("""
                SELECT d.id, d.name, d.title,
                       COUNT(DISTINCT a.id) AS appointment_count,
                       COUNT(DISTINCT vr.id) AS visit_count,
                       COUNT(DISTINCT COALESCE(a.patient_id, vr.patient_id)) AS patient_count
                FROM doctor d
                LEFT JOIN appointment a ON a.doctor_id = d.id AND %s
                LEFT JOIN visit_record vr ON vr.doctor_id = d.id AND %s
                GROUP BY d.id, d.name, d.title
                ORDER BY appointment_count DESC, visit_count DESC, d.id
                """.formatted(appointmentDateFilter, visitDateFilter)),
            "assessmentLevelDistribution", jdbc.queryForList("""
                SELECT assessment_level, COUNT(*) AS total
                FROM patient
                GROUP BY assessment_level
                ORDER BY total DESC, assessment_level
                """),
            "patientActivity", jdbc.queryForList("""
                SELECT p.id, p.name, p.assessment_level,
                       COUNT(DISTINCT a.id) AS appointment_count,
                       COUNT(DISTINCT vr.id) AS visit_count,
                       COUNT(DISTINCT COALESCE(a.doctor_id, vr.doctor_id)) AS doctor_count
                FROM patient p
                LEFT JOIN appointment a ON a.patient_id = p.id AND %s
                LEFT JOIN visit_record vr ON vr.patient_id = p.id AND %s
                GROUP BY p.id, p.name, p.assessment_level
                HAVING appointment_count > 0 OR visit_count > 0
                ORDER BY appointment_count DESC, visit_count DESC, p.id
                """.formatted(appointmentDateFilter, visitDateFilter)),
            "doctorPatientPairs", jdbc.queryForList("""
                SELECT d.name AS doctor_name, p.name AS patient_name,
                       COUNT(DISTINCT a.id) AS appointment_count,
                       COUNT(DISTINCT vr.id) AS visit_count
                FROM doctor d
                JOIN patient p
                LEFT JOIN appointment a ON a.doctor_id = d.id AND a.patient_id = p.id AND %s
                LEFT JOIN visit_record vr ON vr.doctor_id = d.id AND vr.patient_id = p.id AND %s
                GROUP BY d.id, d.name, p.id, p.name
                HAVING appointment_count > 0 OR visit_count > 0
                ORDER BY appointment_count DESC, visit_count DESC, d.name, p.name
                """.formatted(appointmentDateFilter, visitDateFilter)),
            "operationSignals", Map.of(
                "sensitiveViews", countAuditOperation("VIEW_SENSITIVE", auditDateFilter),
                "profileUpdates", countAuditOperation("UPDATE_PROFILE", auditDateFilter),
                "appointmentCreates", countAuditOperation("CREATE_APPOINTMENT", auditDateFilter)
            ),
            "auditSummary", Map.of(
                "sensitiveViews", countAuditOperation("VIEW_SENSITIVE"),
                "profileUpdates", countAuditOperation("UPDATE_PROFILE"),
                "appointmentCreates", countAuditOperation("CREATE_APPOINTMENT")
            )
        );
    }

    @GetMapping("/admin/statistics-basic")
    Map<String, Object> adminStatistics() {
        return Map.of(
            "overview", Map.of(
                "users", count("app_user"),
                "doctors", count("doctor"),
                "patients", count("patient"),
                "appointments", count("appointment"),
                "visitRecords", count("visit_record"),
                "auditLogs", count("audit_log")
            ),
            "appointmentStatusDistribution", jdbc.queryForList("""
                SELECT status, COUNT(*) AS total
                FROM appointment
                GROUP BY status
                ORDER BY total DESC, status
                """),
            "doctorWorkload", jdbc.queryForList("""
                SELECT d.id, d.name, d.title,
                       COUNT(a.id) AS appointment_count,
                       SUM(CASE WHEN a.status = '咨询已结束' THEN 1 ELSE 0 END) AS finished_count,
                       COUNT(DISTINCT a.patient_id) AS patient_count
                FROM doctor d
                LEFT JOIN appointment a ON a.doctor_id = d.id
                GROUP BY d.id, d.name, d.title
                ORDER BY appointment_count DESC, d.id
                """),
            "assessmentLevelDistribution", jdbc.queryForList("""
                SELECT assessment_level, COUNT(*) AS total
                FROM patient
                GROUP BY assessment_level
                ORDER BY total DESC, assessment_level
                """),
            "auditSummary", Map.of(
                "sensitiveViews", countAuditOperation("VIEW_SENSITIVE"),
                "profileUpdates", countAuditOperation("UPDATE_PROFILE"),
                "appointmentCreates", countAuditOperation("CREATE_APPOINTMENT")
            )
        );
    }

    @GetMapping("/appointments")
    List<Map<String, Object>> appointments(Authentication auth) {
        var user = currentUser(auth);
        String role = (String) user.get("role");
        if ("ADMIN".equals(role)) {
            return queryAppointments("");
        }
        if ("DOCTOR".equals(role)) {
            return queryAppointments("WHERE d.user_id = " + user.get("id"));
        }
        if ("COUNSELOR".equals(role)) {
            return queryAppointments("""
                WHERE p.id IN (
                  SELECT pc.patient_id
                  FROM patient_counselor pc
                  JOIN counselor c ON c.id = pc.counselor_id
                  WHERE c.user_id = %s AND pc.active = 1
                )
                """.formatted(user.get("id")));
        }
        return queryAppointments("WHERE p.user_id = " + user.get("id"));
    }

    @PostMapping("/appointments")
    Map<String, Object> createAppointment(Authentication auth, @RequestBody AppointmentCreateRequest request,
                                          HttpServletRequest httpRequest) {
        var user = currentUser(auth);
        if (request == null || request.doctorId() == null || isBlank(request.topic()) || isBlank(request.appointmentTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "预约信息不完整");
        }
        String reason = requireReason(request.reason(), "预约原因不能为空");

        Long patientId = resolvePatientId(user, request.patientId());
        var patientRows = jdbc.queryForList("SELECT id, name FROM patient WHERE id = ?", patientId);
        if (patientRows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "来访者档案不存在");
        }
        var doctorRows = jdbc.queryForList("SELECT id, name FROM doctor WHERE id = ?", request.doctorId());
        if (doctorRows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "咨询师不存在");
        }

        String appointmentTime = request.appointmentTime().trim().replace("T", " ");
        if (appointmentTime.length() == 16) {
            appointmentTime = appointmentTime + ":00";
        }
        String location = isBlank(request.location()) ? "心桥心理咨询室" : request.location().trim();
        var patient = patientRows.get(0);
        var doctor = doctorRows.get(0);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        String finalAppointmentTime = appointmentTime;
        jdbc.update(connection -> {
            var statement = connection.prepareStatement("""
                INSERT INTO appointment(patient_id, doctor_id, topic, appointment_time, status, location)
                VALUES (?, ?, ?, ?, '静候确认', ?)
                """, java.sql.Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, patientId);
            statement.setLong(2, request.doctorId());
            statement.setString(3, request.topic().trim());
            statement.setString(4, finalAppointmentTime);
            statement.setString(5, location);
            return statement;
        }, keyHolder);

        Long appointmentId = keyHolder.getKey() == null ? null : keyHolder.getKey().longValue();
        String newValue = """
            {"patient":"%s","doctor":"%s","topic":"%s","appointment_time":"%s","status":"静候确认","location":"%s"}
            """.formatted(
            patient.get("name"),
            doctor.get("name"),
            request.topic().trim(),
            finalAppointmentTime,
            location
        ).trim();
        auditLog.log(auth, "CREATE_APPOINTMENT", "appointment", appointmentId,
            patient.get("name") + "提交预约申请",
            null, newValue, reason, clientIp(httpRequest));
        notifyCounselorsForAppointment(user, patientId, patient, doctor, finalAppointmentTime, request.topic().trim());

        return Map.of(
            "id", appointmentId,
            "status", "静候确认",
            "message", "预约申请已提交，等待咨询师确认"
        );
    }

    @GetMapping("/patients")
    List<Map<String, Object>> patients(Authentication auth) {
        var user = currentUser(auth);
        String role = (String) user.get("role");
        if ("ADMIN".equals(role)) {
            return jdbc.queryForList("""
                SELECT id, user_id, name, student_no, college, grade,
                       primary_topic, assessment_level, follow_plan, created_at
                FROM patient
                ORDER BY id
                """);
        }
        if ("DOCTOR".equals(role)) {
            return jdbc.queryForList("""
                SELECT DISTINCT p.id, p.name, p.primary_topic, p.assessment_level, p.follow_plan
                FROM patient p
                JOIN appointment a ON a.patient_id = p.id
                JOIN doctor d ON d.id = a.doctor_id
                WHERE d.user_id = ?
                ORDER BY p.id
                """, user.get("id"));
        }
        if ("COUNSELOR".equals(role)) {
            return jdbc.queryForList("""
                SELECT DISTINCT p.id, p.name, p.student_no, p.college, p.grade,
                       p.primary_topic, p.assessment_level, p.follow_plan
                FROM patient p
                JOIN patient_counselor pc ON pc.patient_id = p.id AND pc.active = 1
                JOIN counselor c ON c.id = pc.counselor_id
                WHERE c.user_id = ?
                ORDER BY p.assessment_level DESC, p.id
                """, user.get("id"));
        }
        return jdbc.queryForList("""
            SELECT id, user_id, name, student_no, college, grade,
                   primary_topic, assessment_level, follow_plan, created_at
            FROM patient
            WHERE user_id = ?
            """, user.get("id"));
    }

    @GetMapping("/doctors")
    List<Map<String, Object>> doctors(Authentication auth) {
        var user = currentUser(auth);
        String role = (String) user.get("role");
        if ("ADMIN".equals(role)) {
            return jdbc.queryForList("""
                SELECT id, user_id, name, title, specialties, schedule_note
                FROM doctor
                ORDER BY id
                """);
        }
        if ("DOCTOR".equals(role)) {
            return jdbc.queryForList("""
                SELECT id, user_id, name, title, specialties, schedule_note
                FROM doctor
                WHERE user_id = ?
                ORDER BY id
                """, user.get("id"));
        }
        if ("PATIENT".equals(role)) {
            return jdbc.queryForList("""
                SELECT id, user_id, name, title, specialties, schedule_note
                FROM doctor
                ORDER BY id
                """);
        }
        return jdbc.queryForList("""
            SELECT DISTINCT d.id, d.user_id, d.name, d.title, d.specialties, d.schedule_note
            FROM doctor d
            JOIN appointment a ON a.doctor_id = d.id
            JOIN patient p ON p.id = a.patient_id
            WHERE p.user_id = ?
            ORDER BY d.id
            """, user.get("id"));
    }

    @GetMapping("/admin/users")
    List<Map<String, Object>> users() {
        return jdbc.queryForList("""
            SELECT u.id, u.username, u.display_name, u.role, u.enabled, u.created_at,
                   up.phone, up.email, up.department, up.office_location,
                   up.emergency_contact, up.preference_note, up.bio
            FROM app_user u
            LEFT JOIN user_profile up ON up.user_id = u.id
            ORDER BY u.id
            """);
    }

    @PutMapping("/admin/users/{id}")
    Map<String, Object> updateUser(@PathVariable long id, @RequestBody AdminUserUpdateRequest request,
                                   Authentication auth, HttpServletRequest httpRequest) {
        if (request == null || isBlank(request.displayName()) || isBlank(request.role())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "显示名和角色不能为空");
        }
        var before = userById(id);
        jdbc.update("""
            UPDATE app_user
            SET display_name = ?, role = ?, enabled = ?
            WHERE id = ?
            """,
            request.displayName().trim(),
            normalizeManagedRole(request.role()),
            request.enabled() == null || request.enabled(),
            id
        );
        jdbc.update("""
            INSERT INTO user_profile(user_id, phone, email, department, office_location, emergency_contact, preference_note, bio)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
              phone = VALUES(phone), email = VALUES(email), department = VALUES(department),
              office_location = VALUES(office_location), emergency_contact = VALUES(emergency_contact),
              preference_note = VALUES(preference_note), bio = VALUES(bio)
            """,
            id,
            nullIfBlank(request.phone()),
            nullIfBlank(request.email()),
            nullIfBlank(request.department()),
            nullIfBlank(request.officeLocation()),
            nullIfBlank(request.emergencyContact()),
            nullIfBlank(request.preferenceNote()),
            nullIfBlank(request.bio())
        );
        auditLog.log(auth, "ADMIN_UPDATE_USER", "user", id,
            "管理员修改用户 " + before.get("username"),
            String.valueOf(before), String.valueOf(userById(id)),
            requireReason(request.reason(), "管理员修改用户必须填写原因"), clientIp(httpRequest));
        return Map.of("success", true, "user", userById(id));
    }

    @PostMapping("/admin/users/{id}/notify")
    Map<String, Object> notifyUser(@PathVariable long id, @RequestBody AdminNotifyRequest request,
                                   Authentication auth, HttpServletRequest httpRequest) {
        var admin = currentUser(auth);
        var target = userById(id);
        if (request == null || isBlank(request.title()) || isBlank(request.content())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "通知标题和内容不能为空");
        }
        jdbc.update("""
            INSERT INTO site_message(sender_id, receiver_id, title, content, is_read)
            VALUES (?, ?, ?, ?, 0)
            """, admin.get("id"), id, request.title().trim(), request.content().trim());
        auditLog.log(auth, "ADMIN_NOTIFY_USER", "user", id,
            "管理员向 " + target.get("username") + " 发送通知",
            null, request.title().trim(),
            requireReason(request.reason(), "发送通知必须填写原因"), clientIp(httpRequest));
        return Map.of("success", true);
    }

    @PostMapping("/account/password")
    Map<String, Object> changePassword(Authentication auth, @RequestBody ChangePasswordRequest request,
                                       HttpServletRequest httpRequest) {
        var user = currentUser(auth);
        if (request == null || isBlank(request.oldPassword()) || isBlank(request.newPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "原密码和新密码不能为空");
        }
        var rows = jdbc.queryForList("SELECT password_hash FROM app_user WHERE id = ?", user.get("id"));
        if (rows.isEmpty() || !passwordEncoder.matches(request.oldPassword(), (String) rows.get(0).get("password_hash"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "原密码不正确");
        }
        jdbc.update("UPDATE app_user SET password_hash = ? WHERE id = ?", passwordEncoder.encode(request.newPassword()), user.get("id"));
        auditLog.log(auth, "CHANGE_PASSWORD", "user", ((Number) user.get("id")).longValue(),
            user.get("username") + " 修改密码", null, null, "用户自行修改密码", clientIp(httpRequest));
        return Map.of("success", true);
    }

    @GetMapping("/admin/rbac")
    Map<String, Object> rbacMatrix() {
        return Map.of(
            "roles", jdbc.queryForList("""
                SELECT code, name, description
                FROM rbac_role
                ORDER BY FIELD(code, 'ADMIN', 'DOCTOR', 'COUNSELOR', 'PATIENT'), code
                """),
            "permissions", jdbc.queryForList("""
                SELECT code, name, module, description
                FROM permission
                ORDER BY module, code
                """),
            "rolePermissions", jdbc.queryForList("""
                SELECT rp.role_code, rp.permission_code, p.name AS permission_name, p.module
                FROM role_permission rp
                JOIN permission p ON p.code = rp.permission_code
                ORDER BY rp.role_code, p.module, rp.permission_code
                """)
        );
    }

    @PostMapping("/admin/rbac/{roleCode}/{permissionCode}")
    Map<String, Object> grantPermission(@PathVariable String roleCode, @PathVariable String permissionCode,
                                        @RequestBody(required = false) RbacChangeRequest request,
                                        Authentication auth, HttpServletRequest httpRequest) {
        jdbc.update("INSERT IGNORE INTO role_permission(role_code, permission_code) VALUES (?, ?)", roleCode, permissionCode);
        auditLog.log(auth, "GRANT_PERMISSION", "rbac", null,
            roleCode + " 获得权限 " + permissionCode, null, null,
            requireReason(request == null ? null : request.reason(), "权限调整必须填写原因"), clientIp(httpRequest));
        return Map.of("success", true);
    }

    @DeleteMapping("/admin/rbac/{roleCode}/{permissionCode}")
    Map<String, Object> revokePermission(@PathVariable String roleCode, @PathVariable String permissionCode,
                                         @RequestBody(required = false) RbacChangeRequest request,
                                         Authentication auth, HttpServletRequest httpRequest) {
        jdbc.update("DELETE FROM role_permission WHERE role_code = ? AND permission_code = ?", roleCode, permissionCode);
        auditLog.log(auth, "REVOKE_PERMISSION", "rbac", null,
            roleCode + " 移除权限 " + permissionCode, null, null,
            requireReason(request == null ? null : request.reason(), "权限调整必须填写原因"), clientIp(httpRequest));
        return Map.of("success", true);
    }

    @GetMapping("/patients/{patientId}/history")
    List<Map<String, Object>> patientHistory(@PathVariable long patientId,
                                             @RequestParam(defaultValue = "") String reason,
                                             Authentication auth,
                                             HttpServletRequest httpRequest) {
        String normalizedReason = requireReason(reason, "查看敏感咨询历史必须填写操作原因");
        assertCanReadPatient(patientId, auth);
        var patientRows = jdbc.queryForList("SELECT name FROM patient WHERE id = ?", patientId);
        String patientName = patientRows.isEmpty() ? String.valueOf(patientId) : String.valueOf(patientRows.get(0).get("name"));
        auditLog.log(auth, "VIEW_SENSITIVE", "patient", patientId,
            "查看" + patientName + "的咨询历史",
            null, null, normalizedReason, clientIp(httpRequest));
        return jdbc.queryForList("""
            SELECT vr.id, vr.visit_time, vr.diagnosis_summary, vr.treatment_note, vr.next_plan,
                   d.name AS doctor_name
            FROM visit_record vr
            JOIN doctor d ON d.id = vr.doctor_id
            WHERE vr.patient_id = ?
            ORDER BY vr.id DESC
            """, patientId);
    }

    @GetMapping("/visit-records")
    List<Map<String, Object>> visitRecords(Authentication auth) {
        var user = currentUser(auth);
        String role = (String) user.get("role");
        if ("ADMIN".equals(role)) {
            return queryVisitRecords("");
        }
        if ("DOCTOR".equals(role)) {
            return queryVisitRecords("WHERE d.user_id = " + user.get("id"));
        }
        if ("COUNSELOR".equals(role)) {
            return queryVisitRecords("""
                WHERE p.id IN (
                  SELECT pc.patient_id
                  FROM patient_counselor pc
                  JOIN counselor c ON c.id = pc.counselor_id
                  WHERE c.user_id = %s AND pc.active = 1
                )
                """.formatted(user.get("id")));
        }
        return queryVisitRecords("WHERE p.user_id = " + user.get("id"));
    }

    private List<Map<String, Object>> queryAppointments(String whereClause) {
        return jdbc.queryForList("""
            SELECT a.id, a.topic, a.appointment_time, a.status, a.location,
                   p.id AS patient_id, p.name AS patient_name,
                   d.id AS doctor_id, d.name AS doctor_name
            FROM appointment a
            JOIN patient p ON p.id = a.patient_id
            JOIN doctor d ON d.id = a.doctor_id
            %s
            ORDER BY a.id
            """.formatted(whereClause));
    }

    private List<Map<String, Object>> queryVisitRecords(String whereClause) {
        return jdbc.queryForList("""
            SELECT vr.id, vr.visit_time, vr.diagnosis_summary, vr.next_plan,
                   p.id AS patient_id, p.name AS patient_name,
                   d.id AS doctor_id, d.name AS doctor_name
            FROM visit_record vr
            JOIN patient p ON p.id = vr.patient_id
            JOIN doctor d ON d.id = vr.doctor_id
            %s
            ORDER BY vr.id DESC
            """.formatted(whereClause));
    }

    private void notifyCounselorsForAppointment(Map<String, Object> operator, Long patientId,
                                                Map<String, Object> patient, Map<String, Object> doctor,
                                                String appointmentTime, String topic) {
        var counselors = jdbc.queryForList("""
            SELECT u.id AS user_id, c.name
            FROM patient_counselor pc
            JOIN counselor c ON c.id = pc.counselor_id
            JOIN app_user u ON u.id = c.user_id
            WHERE pc.patient_id = ? AND pc.active = 1
            """, patientId);
        for (var counselor : counselors) {
            jdbc.update("""
                INSERT INTO site_message(sender_id, receiver_id, title, content, is_read)
                VALUES (?, ?, ?, ?, 0)
                """,
                operator.get("id"),
                counselor.get("user_id"),
                "学生预约联动通知",
                "你关注的学生 " + patient.get("name") + " 发起了心理咨询预约；咨询师："
                    + doctor.get("name") + "；时间：" + appointmentTime + "；主题：" + topic
            );
        }
    }

    private Map<String, Object> currentUser(Authentication auth) {
        return jdbc.queryForMap(
            "SELECT id, username, display_name, role FROM app_user WHERE username = ?",
            auth.getName()
        );
    }

    private Map<String, Object> userById(long id) {
        var rows = jdbc.queryForList("""
            SELECT id, username, display_name, role, enabled, created_at
            FROM app_user
            WHERE id = ?
            """, id);
        return rows.isEmpty() ? Map.of() : rows.get(0);
    }

    private Map<String, Object> userProfile(Map<String, Object> user) {
        var profileRows = jdbc.queryForList("""
            SELECT phone, email, department, office_location AS officeLocation,
                   emergency_contact AS emergencyContact, preference_note AS preferenceNote, bio
            FROM user_profile
            WHERE user_id = ?
            """, user.get("id"));

        Map<String, Object> roleProfile = Map.of();
        String role = (String) user.get("role");
        if ("DOCTOR".equals(role)) {
            roleProfile = firstOrEmpty(jdbc.queryForList("""
                SELECT id AS doctorId, name, title, specialties, schedule_note AS scheduleNote
                FROM doctor
                WHERE user_id = ?
                """, user.get("id")));
        } else if ("PATIENT".equals(role)) {
            roleProfile = firstOrEmpty(jdbc.queryForList("""
                SELECT id AS patientId, name, student_no AS studentNo, college, grade,
                       primary_topic AS primaryTopic, assessment_level AS assessmentLevel, follow_plan AS followPlan
                FROM patient
                WHERE user_id = ?
                """, user.get("id")));
        }

        return Map.of(
            "user", Map.of(
                "id", user.get("id"),
                "username", user.get("username"),
                "displayName", user.get("display_name"),
                "role", role,
                "roleName", roleName(role)
            ),
            "profile", firstOrEmpty(profileRows),
            "roleProfile", roleProfile
        );
    }

    private void assertCanReadPatient(long patientId, Authentication auth) {
        var user = currentUser(auth);
        String role = (String) user.get("role");
        if ("ADMIN".equals(role)) return;

        Integer allowed;
        if ("DOCTOR".equals(role)) {
            allowed = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM appointment a
                JOIN doctor d ON d.id = a.doctor_id
                WHERE a.patient_id = ? AND d.user_id = ?
                """, Integer.class, patientId, user.get("id"));
        } else if ("COUNSELOR".equals(role)) {
            allowed = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM patient_counselor pc
                JOIN counselor c ON c.id = pc.counselor_id
                WHERE pc.patient_id = ? AND c.user_id = ? AND pc.active = 1
                """, Integer.class, patientId, user.get("id"));
        } else {
            allowed = jdbc.queryForObject(
                "SELECT COUNT(*) FROM patient WHERE id = ? AND user_id = ?",
                Integer.class,
                patientId,
                user.get("id")
            );
        }
        if (allowed == null || allowed == 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前身份无权查看该来访者既往史");
        }
    }

    private Integer count(String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }

    private Integer scalarCount(String sql) {
        return jdbc.queryForObject(sql, Integer.class);
    }

    private Integer countAuditOperation(String operationType) {
        return jdbc.queryForObject(
            "SELECT COUNT(*) FROM audit_log WHERE operation_type = ?",
            Integer.class,
            operationType
        );
    }

    private Integer countAuditOperation(String operationType, String dateFilter) {
        return jdbc.queryForObject(
            "SELECT COUNT(*) FROM audit_log WHERE operation_type = ? AND " + dateFilter,
            Integer.class,
            operationType
        );
    }

    private String normalizeRange(String range) {
        if ("day".equalsIgnoreCase(range)) return "day";
        if ("week".equalsIgnoreCase(range)) return "week";
        return "month";
    }

    private String rangeSql(String column, String range) {
        if ("day".equals(range)) {
            return column + " >= CURDATE()";
        }
        if ("week".equals(range)) {
            return column + " >= DATE_SUB(CURDATE(), INTERVAL 6 DAY)";
        }
        return column + " >= DATE_SUB(CURDATE(), INTERVAL 29 DAY)";
    }

    private String periodLabel(String range) {
        if ("day".equals(range)) return "今日统计";
        if ("week".equals(range)) return "近 7 日统计";
        return "近 30 日统计";
    }

    private String normalizeRegisterRole(String role) {
        if ("PATIENT".equalsIgnoreCase(role)) return "PATIENT";
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "只能注册来访者账号");
    }

    private String normalizeManagedRole(String role) {
        if ("ADMIN".equalsIgnoreCase(role)) return "ADMIN";
        if ("DOCTOR".equalsIgnoreCase(role)) return "DOCTOR";
        if ("COUNSELOR".equalsIgnoreCase(role)) return "COUNSELOR";
        if ("PATIENT".equalsIgnoreCase(role)) return "PATIENT";
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无效角色");
    }

    private Map<String, Object> firstOrEmpty(List<Map<String, Object>> rows) {
        return rows.isEmpty() ? Map.of() : rows.get(0);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String nullIfBlank(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private String requireReason(String value, String message) {
        if (isBlank(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return value.trim();
    }

    private String clientIp(HttpServletRequest request) {
        if (request == null) return null;
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (!isBlank(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private Long resolvePatientId(Map<String, Object> user, Long requestedPatientId) {
        String role = (String) user.get("role");
        if ("PATIENT".equals(role)) {
            var rows = jdbc.queryForList("SELECT id FROM patient WHERE user_id = ?", user.get("id"));
            if (rows.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "当前账号未绑定来访者档案");
            }
            return ((Number) rows.get(0).get("id")).longValue();
        }
        if ("ADMIN".equals(role)) {
            if (requestedPatientId == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "管理员安排预约时必须指定来访者");
            }
            return requestedPatientId;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前身份不能提交预约申请");
    }

    @GetMapping("/admin/audit-logs")
    List<Map<String, Object>> auditLogs(
            @RequestParam(defaultValue = "") String operationType,
            @RequestParam(defaultValue = "") String targetType,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "100") int limit) {
        return auditLog.queryLogs(operationType, targetType, limit, offset);
    }

    @GetMapping("/admin/audit-logs/count")
    Map<String, Object> auditLogsCount(
            @RequestParam(defaultValue = "") String operationType,
            @RequestParam(defaultValue = "") String targetType) {
        return Map.of("total", auditLog.countLogs(operationType, targetType));
    }

    @GetMapping("/admin/audit-logs/{id}")
    Map<String, Object> auditLogDetail(@PathVariable long id) {
        var log = auditLog.getLogDetail(id);
        if (log.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "审计日志不存在");
        }
        return log;
    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/health")
    Map<String, String> health() {
        return Map.of("status", "ok");
    }

    private Authentication authForUsername(String username) {
        return new Authentication() {
            @Override public String getName() { return username; }
            @Override public java.util.Collection<? extends org.springframework.security.core.GrantedAuthority> getAuthorities() { return List.of(); }
            @Override public Object getCredentials() { return null; }
            @Override public Object getDetails() { return null; }
            @Override public Object getPrincipal() { return username; }
            @Override public boolean isAuthenticated() { return true; }
            @Override public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {}
        };
    }

    private Map<String, Object> sessionPayload(Map<String, Object> user) {
        String role = (String) user.get("role");
        return Map.of(
            "token", "prototype-" + UUID.randomUUID(),
            "authType", "prototype",
            "user", Map.of(
                "id", user.get("id"),
                "username", user.get("username"),
                "displayName", user.get("display_name"),
                "role", role,
                "roleName", roleNameV2(role)
            ),
            "workspace", workspaceV2(role),
            "navigation", navigationV2(role),
            "dashboardCards", dashboardCardsV2(role, user),
            "permissions", permissionsV2(role)
        );
    }

    private Map<String, String> workspace(String role) {
        if ("ADMIN".equals(role)) {
            return Map.of(
                "title", "管理员工作台",
                "subtitle", "统筹预约、咨询师、来访者档案和系统用户",
                "primaryAction", "安排咨询"
            );
        }
        if ("DOCTOR".equals(role)) {
            return Map.of(
                "title", "医生工作台",
                "subtitle", "查看我的预约、来访者跟进和咨询记录",
                "primaryAction", "填写咨询记录"
            );
        }
        return Map.of(
            "title", "我的咨询空间",
            "subtitle", "查看我的预约、跟进建议和咨询记录",
            "primaryAction", "预约咨询"
        );
    }

    private List<Map<String, String>> navigation(String role) {
        if ("ADMIN".equals(role)) {
            return List.of(
                nav("dashboard", "概览大厅"),
                nav("appointments", "预约管理"),
                nav("doctors", "咨询师管理"),
                nav("patients", "来访者档案"),
                nav("visitRecords", "咨询记录"),
                nav("users", "系统用户"),
                nav("messages", "站内信箱"),
                nav("auditLogs", "审计日志"),
                nav("statistics", "数据统计"),
                nav("profile", "个人资料")
            );
        }
        if ("DOCTOR".equals(role)) {
            return List.of(
                nav("dashboard", "我的首页"),
                nav("appointments", "我的预约"),
                nav("patients", "我的来访者"),
                nav("visitRecords", "咨询记录"),
                nav("followPlans", "跟进计划"),
                nav("messages", "站内信箱"),
                nav("profile", "个人资料")
            );
        }
        return List.of(
            nav("dashboard", "我的首页"),
            nav("appointments", "我的预约"),
            nav("bookings", "预约咨询"),
            nav("visitRecords", "我的咨询记录"),
            nav("followPlans", "跟进计划"),
                nav("messages", "站内信箱"),
            nav("profile", "个人资料")
        );
    }

    private List<Map<String, Object>> dashboardCards(String role, Map<String, Object> user) {
        if ("ADMIN".equals(role)) {
            return List.of(
                card("todayAppointments", "今日预约", count("appointment"), "人次"),
                card("pendingAppointments", "待确认预约", countByStatus("静候确认"), "人次"),
                card("doctors", "咨询师", count("doctor"), "人"),
                card("patients", "来访者", count("patient"), "人"),
                card("users", "系统用户", count("app_user"), "个"),
                card("health", "系统健康", "正常", ""), card("unreadMessages", "未读消息", countUnreadMessages(user.get("id")), "封")
            );
        }
        if ("DOCTOR".equals(role)) {
            Object userId = user.get("id");
            return List.of(
                card("myAppointments", "我的预约", countDoctorAppointments(userId), "次"),
                card("pendingRecords", "待填写记录", countDoctorFinishedAppointments(userId), "条"),
                card("focusPatients", "重点关注来访者", countDoctorFocusPatients(userId), "人"),
                card("nextFollowUp", "下一次跟进", "2 天后", ""), card("unreadMessages", "未读消息", countUnreadMessages(user.get("id")), "封")
            );
        }
        Object userId = user.get("id");
        return List.of(
            card("myAppointments", "我的预约", countPatientAppointments(userId), "次"),
            card("nextAppointment", "下一次预约", nextPatientAppointment(userId), ""),
            card("followPlan", "跟进建议", patientFollowPlan(userId), ""),
            card("records", "咨询记录", countPatientVisitRecords(userId), "条"), card("unreadMessages", "未读信件", countUnreadMessages(userId), "封")
        );
    }

    private Map<String, List<String>> permissions(String role) {
        if ("ADMIN".equals(role)) {
            return Map.of(
                "canView", List.of("allAppointments", "allDoctors", "allPatients", "allVisitRecords", "users", "statistics", "ownProfile"),
                "canManage", List.of("appointments", "doctors", "patients", "users", "ownProfile")
            );
        }
        if ("DOCTOR".equals(role)) {
            return Map.of(
                "canView", List.of("ownAppointments", "assignedPatients", "ownVisitRecords", "ownProfile"),
                "canManage", List.of("visitRecords", "followPlans")
            );
        }
        return Map.of(
            "canView", List.of("ownAppointments", "ownProfile", "ownVisitRecords", "ownFollowPlans"),
            "canManage", List.of("bookingRequests", "ownProfile")
        );
    }

    private Map<String, String> nav(String key, String label) {
        return Map.of("key", key, "label", label);
    }

    private Map<String, Object> card(String key, String label, Object value, String unit) {
        return Map.of("key", key, "label", label, "value", value, "unit", unit);
    }

    private Map<String, String> workspaceV2(String role) {
        if ("ADMIN".equals(role)) {
            return Map.of("title", "管理员工作台", "subtitle", "统筹预约、咨询师、来访者档案和系统用户", "primaryAction", "安排咨询");
        }
        if ("DOCTOR".equals(role)) {
            return Map.of("title", "咨询师工作台", "subtitle", "查看我的预约、来访者跟进和咨询记录", "primaryAction", "填写咨询记录");
        }
        if ("COUNSELOR".equals(role)) {
            return Map.of("title", "辅导员工作台", "subtitle", "关注学生风险信号、预约联动和站内通知", "primaryAction", "查看关注学生");
        }
        return Map.of("title", "我的咨询空间", "subtitle", "查看我的预约、跟进建议和咨询记录", "primaryAction", "预约咨询");
    }

    private List<Map<String, String>> navigationV2(String role) {
        if ("ADMIN".equals(role)) {
            return List.of(
                nav("dashboard", "概览大厅"),
                nav("appointments", "预约管理"),
                nav("doctors", "咨询师管理"),
                nav("patients", "来访者档案"),
                nav("visitRecords", "咨询记录"),
                nav("users", "系统用户"),
                nav("rbac", "权限管理"),
                nav("messages", "站内信箱"),
                nav("auditLogs", "审计日志"),
                nav("statistics", "数据统计"),
                nav("profile", "个人资料")
            );
        }
        if ("DOCTOR".equals(role)) {
            return List.of(
                nav("dashboard", "我的首页"),
                nav("appointments", "我的预约"),
                nav("patients", "我的来访者"),
                nav("visitRecords", "咨询记录"),
                nav("followPlans", "跟进计划"),
                nav("messages", "站内信箱"),
                nav("profile", "个人资料")
            );
        }
        if ("COUNSELOR".equals(role)) {
            return List.of(
                nav("dashboard", "我的首页"),
                nav("patients", "关注学生"),
                nav("appointments", "学生预约"),
                nav("messages", "站内信箱"),
                nav("profile", "个人资料")
            );
        }
        return List.of(
            nav("dashboard", "我的首页"),
            nav("appointments", "我的预约"),
            nav("bookings", "预约咨询"),
            nav("visitRecords", "我的咨询记录"),
            nav("followPlans", "跟进计划"),
            nav("messages", "站内信箱"),
            nav("profile", "个人资料")
        );
    }

    private List<Map<String, Object>> dashboardCardsV2(String role, Map<String, Object> user) {
        if ("ADMIN".equals(role)) {
            return List.of(
                card("todayAppointments", "今日预约", count("appointment"), "人次"),
                card("pendingAppointments", "待确认预约", countByStatus("静候确认"), "人次"),
                card("doctors", "咨询师", count("doctor"), "人"),
                card("patients", "来访者", count("patient"), "人"),
                card("users", "系统用户", count("app_user"), "个"),
                card("health", "系统健康", "正常", ""),
                card("unreadMessages", "未读消息", countUnreadMessages(user.get("id")), "封")
            );
        }
        if ("DOCTOR".equals(role)) {
            Object userId = user.get("id");
            return List.of(
                card("myAppointments", "我的预约", countDoctorAppointments(userId), "次"),
                card("pendingRecords", "待填写记录", countDoctorFinishedAppointments(userId), "条"),
                card("focusPatients", "重点关注来访者", countDoctorFocusPatients(userId), "人"),
                card("unreadMessages", "未读消息", countUnreadMessages(userId), "封")
            );
        }
        if ("COUNSELOR".equals(role)) {
            Object userId = user.get("id");
            return List.of(
                card("assignedPatients", "关注学生", countCounselorPatients(userId), "人"),
                card("focusPatients", "关注/重点等级", countCounselorFocusPatients(userId), "人"),
                card("relatedAppointments", "学生预约", countCounselorAppointments(userId), "次"),
                card("unreadMessages", "联动通知", countUnreadMessages(userId), "封")
            );
        }
        Object userId = user.get("id");
        return List.of(
            card("myAppointments", "我的预约", countPatientAppointments(userId), "次"),
            card("nextAppointment", "下一次预约", nextPatientAppointment(userId), ""),
            card("followPlan", "跟进建议", patientFollowPlan(userId), ""),
            card("records", "咨询记录", countPatientVisitRecords(userId), "条"),
            card("unreadMessages", "未读信件", countUnreadMessages(userId), "封")
        );
    }

    private Map<String, List<String>> permissionsV2(String role) {
        if ("ADMIN".equals(role)) {
            return Map.of(
                "canView", List.of("allAppointments", "allDoctors", "allPatients", "allVisitRecords", "users", "statistics", "auditLogs", "rbac", "ownProfile"),
                "canManage", List.of("appointments", "doctors", "patients", "users", "rbacReadonly", "ownProfile")
            );
        }
        if ("DOCTOR".equals(role)) {
            return Map.of("canView", List.of("ownAppointments", "assignedPatients", "ownVisitRecords", "ownProfile"), "canManage", List.of("visitRecords", "followPlans"));
        }
        if ("COUNSELOR".equals(role)) {
            return Map.of("canView", List.of("assignedStudents", "studentAppointments", "messages", "ownProfile"), "canManage", List.of("messages", "studentCareFollowup"));
        }
        return Map.of("canView", List.of("ownAppointments", "ownProfile", "ownVisitRecords", "ownFollowPlans"), "canManage", List.of("bookingRequests", "ownProfile"));
    }

    private String roleNameV2(String role) {
        if ("ADMIN".equals(role)) return "管理员";
        if ("DOCTOR".equals(role)) return "咨询师";
        if ("COUNSELOR".equals(role)) return "辅导员";
        return "来访者";
    }

    private String roleName(String role) {
        if ("ADMIN".equals(role)) return "管理员";
        if ("DOCTOR".equals(role)) return "医生";
        return "就诊人员";
    }

    private Integer countByStatus(String status) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM appointment WHERE status = ?", Integer.class, status);
    }

    private Integer countDoctorAppointments(Object userId) {
        return jdbc.queryForObject("""
            SELECT COUNT(*)
            FROM appointment a
            JOIN doctor d ON d.id = a.doctor_id
            WHERE d.user_id = ?
            """, Integer.class, userId);
    }

    private Integer countDoctorFinishedAppointments(Object userId) {
        return jdbc.queryForObject("""
            SELECT COUNT(*)
            FROM appointment a
            JOIN doctor d ON d.id = a.doctor_id
            WHERE d.user_id = ? AND a.status = '咨询已结束'
            """, Integer.class, userId);
    }

    private Integer countDoctorFocusPatients(Object userId) {
        return jdbc.queryForObject("""
            SELECT COUNT(DISTINCT p.id)
            FROM patient p
            JOIN appointment a ON a.patient_id = p.id
            JOIN doctor d ON d.id = a.doctor_id
            WHERE d.user_id = ? AND p.assessment_level IN ('关注', '重点')
            """, Integer.class, userId);
    }

    private Integer countCounselorPatients(Object userId) {
        return jdbc.queryForObject("""
            SELECT COUNT(DISTINCT p.id)
            FROM patient p
            JOIN patient_counselor pc ON pc.patient_id = p.id AND pc.active = 1
            JOIN counselor c ON c.id = pc.counselor_id
            WHERE c.user_id = ?
            """, Integer.class, userId);
    }

    private Integer countCounselorFocusPatients(Object userId) {
        return jdbc.queryForObject("""
            SELECT COUNT(DISTINCT p.id)
            FROM patient p
            JOIN patient_counselor pc ON pc.patient_id = p.id AND pc.active = 1
            JOIN counselor c ON c.id = pc.counselor_id
            WHERE c.user_id = ? AND p.assessment_level IN ('关注', '重点')
            """, Integer.class, userId);
    }

    private Integer countCounselorAppointments(Object userId) {
        return jdbc.queryForObject("""
            SELECT COUNT(*)
            FROM appointment a
            JOIN patient_counselor pc ON pc.patient_id = a.patient_id AND pc.active = 1
            JOIN counselor c ON c.id = pc.counselor_id
            WHERE c.user_id = ?
            """, Integer.class, userId);
    }

    private Integer countPatientAppointments(Object userId) {
        return jdbc.queryForObject("""
            SELECT COUNT(*)
            FROM appointment a
            JOIN patient p ON p.id = a.patient_id
            WHERE p.user_id = ?
            """, Integer.class, userId);
    }

    private String nextPatientAppointment(Object userId) {
        var rows = jdbc.queryForList("""
            SELECT a.appointment_time
            FROM appointment a
            JOIN patient p ON p.id = a.patient_id
            WHERE p.user_id = ? AND a.status <> '咨询已结束'
            ORDER BY a.id
            LIMIT 1
            """, userId);
        return rows.isEmpty() ? "暂无预约" : String.valueOf(rows.get(0).get("appointment_time"));
    }

    private String patientFollowPlan(Object userId) {
        var rows = jdbc.queryForList("SELECT follow_plan FROM patient WHERE user_id = ?", userId);
        return rows.isEmpty() ? "暂无跟进计划" : (String) rows.get(0).get("follow_plan");
    }

    private Integer countPatientVisitRecords(Object userId) {
        return jdbc.queryForObject("""
            SELECT COUNT(*)
            FROM visit_record vr
            JOIN patient p ON p.id = vr.patient_id
            WHERE p.user_id = ?
            """, Integer.class, userId);
    }

    record LoginRequest(String username, String password) {}

    record RegisterRequest(
        String username,
        String password,
        String displayName,
        String name,
        String studentNo,
        String college,
        String grade,
        String primaryTopic
    ) {}

    record AdminUserUpdateRequest(
        String displayName,
        String role,
        Boolean enabled,
        String phone,
        String email,
        String department,
        String officeLocation,
        String emergencyContact,
        String preferenceNote,
        String bio,
        String reason
    ) {}

    record AdminNotifyRequest(String title, String content, String reason) {}

    record ChangePasswordRequest(String oldPassword, String newPassword) {}

    record RbacChangeRequest(String reason) {}

    record AppointmentCreateRequest(
        Long patientId,
        Long doctorId,
        String topic,
        String appointmentTime,
        String location,
        String reason
    ) {}

    record ProfileUpdateRequest(
        String phone,
        String email,
        String department,
        String officeLocation,
        String emergencyContact,
        String preferenceNote,
        String bio
    ) {}

    // ==== 站内信功能追加 ====
    @GetMapping("/messages")
    List<Map<String, Object>> messages(Authentication auth) {
        var user = currentUser(auth);
        return jdbc.queryForList("""
            SELECT m.id, m.title, m.content, m.is_read, m.created_at,
                   s.display_name AS sender_name, s.role AS sender_role
            FROM site_message m
            JOIN app_user s ON s.id = m.sender_id
            WHERE m.receiver_id = ?
            ORDER BY m.id DESC
            """, user.get("id"));
    }

    @PutMapping("/messages/{id}/read")
    Map<String, Object> readMessage(@PathVariable long id, Authentication auth) {
        var user = currentUser(auth);
        int updated = jdbc.update(
            "UPDATE site_message SET is_read = 1 WHERE id = ? AND receiver_id = ?",
            id, user.get("id")
        );
        if (updated == 0) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权操作或信件不存在");
        return Map.of("success", true);
    }

    private Integer countUnreadMessages(Object userId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM site_message WHERE receiver_id = ? AND is_read = 0", Integer.class, userId);
    }

  @PostMapping("/messages")
  Map<String, Object> sendMessage(Authentication auth, @RequestBody SendMessageRequest request) {
    var user = currentUser(auth);
    if (request == null || isBlank(request.title()) || isBlank(request.content()) || isBlank(request.receiverUsername())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "信件内容或收件人不能为空");
    }
    if (request.receiverUsername().equals(user.get("username"))) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不能将信件发送给自己");
    }
    var targetUsers = jdbc.queryForList("SELECT id FROM app_user WHERE username = ?", request.receiverUsername());
    if (targetUsers.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "收件人账号不存在，请检查拼写");
    }

    jdbc.update("""
            INSERT INTO site_message (sender_id, receiver_id, title, content, is_read)
            VALUES (?, ?, ?, ?, 0)
            """, user.get("id"), targetUsers.get(0).get("id"), request.title(), request.content());

    return Map.of("success", true);
  }

  record SendMessageRequest(String receiverUsername, String title, String content) {}
  @GetMapping("/messages/sent")
  List<Map<String, Object>> sentMessages(Authentication auth) {
    var user = currentUser(auth);
    return jdbc.queryForList("""
            SELECT m.id, m.title, m.content, m.is_read, m.created_at,
                   r.display_name AS receiver_name, r.role AS receiver_role
            FROM site_message m
            JOIN app_user r ON r.id = m.receiver_id
            WHERE m.sender_id = ?
            ORDER BY m.id DESC
            """, user.get("id"));
  }
}
