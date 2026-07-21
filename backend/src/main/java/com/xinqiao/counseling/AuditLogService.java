package com.xinqiao.counseling;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {
    private final JdbcTemplate jdbc;

    public AuditLogService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void log(Authentication auth, String operationType, String targetType, Long targetId,
                    String targetDescription, String oldValue, String newValue, String reason) {
        long operatorId = 0;
        String operatorName = "system";

        if (auth != null && auth.isAuthenticated()) {
            operatorName = auth.getName();
            var rows = jdbc.queryForList(
                "SELECT id, display_name FROM app_user WHERE username = ?",
                auth.getName()
            );
            if (!rows.isEmpty()) {
                var user = rows.get(0);
                operatorId = ((Number) user.get("id")).longValue();
                operatorName = (String) user.get("display_name");
            }
        }

        jdbc.update("""
            INSERT INTO audit_log(
              operator_id, operator_name, operation_type, target_type, target_id,
              target_description, old_value, new_value, reason, ip_address
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            operatorId,
            operatorName,
            operationType,
            targetType,
            targetId,
            targetDescription,
            truncate(oldValue, 65535),
            truncate(newValue, 65535),
            truncate(reason, 500),
            null
        );
    }

    public List<Map<String, Object>> queryLogs(String operationType, String targetType, int limit, int offset) {
        String normalizedOperation = normalizeFilter(operationType);
        String normalizedTarget = normalizeFilter(targetType);
        return jdbc.queryForList("""
            SELECT id, operator_name, operation_type, target_type, target_id,
                   target_description, old_value, new_value, reason, ip_address, created_at
            FROM audit_log
            WHERE (? = '' OR operation_type = ?)
              AND (? = '' OR target_type = ?)
            ORDER BY created_at DESC, id DESC
            LIMIT ? OFFSET ?
            """,
            normalizedOperation, normalizedOperation,
            normalizedTarget, normalizedTarget,
            Math.max(1, Math.min(limit, 500)),
            Math.max(0, offset)
        );
    }

    public int countLogs(String operationType, String targetType) {
        String normalizedOperation = normalizeFilter(operationType);
        String normalizedTarget = normalizeFilter(targetType);
        Integer total = jdbc.queryForObject("""
            SELECT COUNT(*)
            FROM audit_log
            WHERE (? = '' OR operation_type = ?)
              AND (? = '' OR target_type = ?)
            """,
            Integer.class,
            normalizedOperation, normalizedOperation,
            normalizedTarget, normalizedTarget
        );
        return total == null ? 0 : total;
    }

    public Map<String, Object> getLogDetail(long id) {
        var rows = jdbc.queryForList("""
            SELECT al.*, au.display_name AS operator_display_name, au.role AS operator_role
            FROM audit_log al
            LEFT JOIN app_user au ON au.id = al.operator_id
            WHERE al.id = ?
            """, id);
        return rows.isEmpty() ? Map.of() : rows.get(0);
    }

    private String normalizeFilter(String value) {
        return value == null ? "" : value.trim();
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) return value;
        return value.substring(0, maxLength);
    }
}
