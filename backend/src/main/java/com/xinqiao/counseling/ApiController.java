package com.xinqiao.counseling;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final JdbcTemplate jdbc;

    public ApiController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/me")
    Map<String, Object> me(Authentication auth) {
        return currentUser(auth);
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
            SELECT a.id, a.topic, a.appointment_time, a.status,
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

    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/health")
    Map<String, String> health() {
        return Map.of("status", "ok");
    }
}
