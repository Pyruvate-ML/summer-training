package com.xinqiao.counseling;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;

    public ApiController(JdbcTemplate jdbc, PasswordEncoder passwordEncoder) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/auth/login")
    Map<String, Object> login(@RequestBody LoginRequest request) {
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
    Map<String, Object> updateProfile(Authentication auth, @RequestBody ProfileUpdateRequest request) {
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
        return queryAppointments("WHERE p.user_id = " + user.get("id"));
    }

    @GetMapping("/patients")
    List<Map<String, Object>> patients(Authentication auth) {
        var user = currentUser(auth);
        String role = (String) user.get("role");
        if ("ADMIN".equals(role)) {
            return jdbc.queryForList("SELECT * FROM patient ORDER BY id");
        }
        if ("DOCTOR".equals(role)) {
            return jdbc.queryForList("""
                SELECT DISTINCT p.*
                FROM patient p
                JOIN appointment a ON a.patient_id = p.id
                JOIN doctor d ON d.id = a.doctor_id
                WHERE d.user_id = ?
                ORDER BY p.id
                """, user.get("id"));
        }
        return jdbc.queryForList("SELECT * FROM patient WHERE user_id = ?", user.get("id"));
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
            SELECT id, username, display_name, role, enabled, created_at
            FROM app_user
            ORDER BY id
            """);
    }

    @GetMapping("/patients/{patientId}/history")
    List<Map<String, Object>> patientHistory(@PathVariable long patientId, Authentication auth) {
        assertCanReadPatient(patientId, auth);
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
            SELECT vr.id, vr.visit_time, vr.diagnosis_summary, vr.treatment_note, vr.next_plan,
                   p.id AS patient_id, p.name AS patient_name,
                   d.id AS doctor_id, d.name AS doctor_name
            FROM visit_record vr
            JOIN patient p ON p.id = vr.patient_id
            JOIN doctor d ON d.id = vr.doctor_id
            %s
            ORDER BY vr.id DESC
            """.formatted(whereClause));
    }

    private Map<String, Object> currentUser(Authentication auth) {
        return jdbc.queryForMap(
            "SELECT id, username, display_name, role FROM app_user WHERE username = ?",
            auth.getName()
        );
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

    private Map<String, Object> firstOrEmpty(List<Map<String, Object>> rows) {
        return rows.isEmpty() ? Map.of() : rows.get(0);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String nullIfBlank(String value) {
        return isBlank(value) ? null : value.trim();
    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/health")
    Map<String, String> health() {
        return Map.of("status", "ok");
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
                "roleName", roleName(role)
            ),
            "workspace", workspace(role),
            "navigation", navigation(role),
            "dashboardCards", dashboardCards(role, user),
            "permissions", permissions(role)
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
                nav("statistics", "数据统计")
            );
        }
        if ("DOCTOR".equals(role)) {
            return List.of(
                nav("dashboard", "我的首页"),
                nav("appointments", "我的预约"),
                nav("patients", "我的来访者"),
                nav("visitRecords", "咨询记录"),
                nav("followPlans", "跟进计划"),
                nav("profile", "个人资料")
            );
        }
        return List.of(
            nav("dashboard", "我的首页"),
            nav("appointments", "我的预约"),
            nav("bookings", "预约咨询"),
            nav("visitRecords", "我的咨询记录"),
            nav("followPlans", "跟进计划"),
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
                card("health", "系统健康", "正常", "")
            );
        }
        if ("DOCTOR".equals(role)) {
            Object userId = user.get("id");
            return List.of(
                card("myAppointments", "我的预约", countDoctorAppointments(userId), "次"),
                card("pendingRecords", "待填写记录", countDoctorFinishedAppointments(userId), "条"),
                card("focusPatients", "重点关注来访者", countDoctorFocusPatients(userId), "人"),
                card("nextFollowUp", "下一次跟进", "2 天后", "")
            );
        }
        Object userId = user.get("id");
        return List.of(
            card("myAppointments", "我的预约", countPatientAppointments(userId), "次"),
            card("nextAppointment", "下一次预约", nextPatientAppointment(userId), ""),
            card("followPlan", "跟进建议", patientFollowPlan(userId), ""),
            card("records", "咨询记录", countPatientVisitRecords(userId), "条")
        );
    }

    private Map<String, List<String>> permissions(String role) {
        if ("ADMIN".equals(role)) {
            return Map.of(
                "canView", List.of("allAppointments", "allDoctors", "allPatients", "allVisitRecords", "users", "statistics"),
                "canManage", List.of("appointments", "doctors", "patients", "users")
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

    record ProfileUpdateRequest(
        String phone,
        String email,
        String department,
        String officeLocation,
        String emergencyContact,
        String preferenceNote,
        String bio
    ) {}
}
