
package com.xinqiao.counseling;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/**
 * 操作日志与审计追踪服务
 * 对所有敏感操作进行统一记录，支持故障排查、责任认定和安全审计。
 */
@Service
public class AuditLogService {

    private final JdbcTemplate jdbc;

    public AuditLogService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * 记录审计日志
     *
     * @param auth     当前认证信息（可为 null，如登录失败场景）
     * @param opType   操作类型：VIEW_SENSITIVE / MODIFY_RISK_LEVEL / MODIFY_APPOINTMENT
     *                 / MODIFY_VISIT_RECORD / MODIFY_USER_PERMISSION / EXPORT_DATA / LOGIN / UPDATE_PROFILE
     * @param targetType  操作对象类型：patient / appointment / visit_record / user_profile / user
     * @param targetId    操作对象 ID
     * @param description 操作对象描述（如："陈同学的档案"）
     * @param oldValue    修改前内容（JSON 或纯文本，查询等无修改操作可传 null）
     * @param newValue    修改后内容
     * @param reason      操作原因
     */
    public void log(Authentication auth, String opType, String targetType,
                    Long targetId, String description,
                    String oldValue, String newValue, String reason) {
        long operatorId;
        String operatorName;

        if (auth != null && auth.isAuthenticated()) {
            try {
                var user = jdbc.queryForMap(
                    "SELECT id, display_name FROM app_user WHERE username = ?",
                    auth.getName()
                );
                operatorId = ((Number) user.get("id")).longValue();
                operatorName = (String) user.get("display_name");
            } catch (Exception e) {
                operatorId = 0;
                operatorName = auth.getName();
            }
        } else {
            operatorId = 0;
            operatorName = "system";
        }

        jdbc.update("""
            INSERT INTO audit_log(
                operator_id, operator_name, operation_type,
                target_type, target_id, target_description,
                old_value, new_value, reason, 
                ip_address, created_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            operatorId,
            operatorName,
            opType,
            targetType,
            targetId,
            description,
            truncate(oldValue, 65535),
            truncate(newValue, 65535),
            reason,
            null, // ip_address - would need HttpServletRequest
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );
    }

    /**
     * 管理员查询全部审计日志（带可选过滤）
     */
    public List<Map<String, Object>> queryLogs(String opType, String targetType, int limit, int offset) {
        String sql = """
            SELECT al.id, al.operator_name, al.operation_type,
                   al.target_type, al.target_id, al.target_description,
                   al.old_value, al.new_value, al.reason,
                   al.ip_address, al.created_at
            FROM audit_log al
            WHERE (? = '' OR al.operation_type = ?)
              AND (? = '' OR al.target_type = ?)
            ORDER BY al.created_at DESC
            LIMIT ? OFFSET ?
            """;
        return jdbc.queryForList(sql,
            opType == null ? "" : opType, opType == null ? "" : opType,
            targetType == null ? "" : targetType, targetType == null ? "" : targetType,
            limit, offset);
    }

    /**
     * 统计审计日志总数
     */
    public int countLogs(String opType, String targetType) {
        String sql = """
            SELECT COUNT(*) FROM audit_log al
            WHERE (? = '' OR al.operation_type = ?)
              AND (? = '' OR al.target_type = ?)
            """;
        Integer count = jdbc.queryForObject(sql, Integer.class,
            opType == null ? "" : opType, opType == null ? "" : opType,
            targetType == null ? "" : targetType, targetType == null ? "" : targetType);
        return count != null ? count : 0;
    }

    /**
     * 查询单条日志详情
     */
    public Map<String, Object> getLogDetail(long logId) {
        var rows = jdbc.queryForList("""
            SELECT al.*, 
                   au.display_name AS operator_display_name,
                   au.role AS operator_role
            FROM audit_log al
            LEFT JOIN app_user au ON au.id = al.operator_id
            WHERE al.id = ?
            """, logId);
        return rows.isEmpty() ? Map.of() : rows.get(0);
    }

    private String truncate(String value, int maxLen) {
        if (value == null) return null;
        return value.length() > maxLen ? value.substring(0, maxLen) : value;
    }
}
