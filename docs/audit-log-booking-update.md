# 操作日志与预约咨询闭环更新说明

本次将 `origin/codex/audit-log-booking` 分支中的有效功能整合进当前 `main` 版本，并保留现有站内信、完整 MySQL schema 和前端资源路径。

## 功能

- 管理员导航新增“审计日志”，可查看操作时间、操作人、操作类型、操作对象和原因。
- 病人点击“预约咨询”可提交预约申请。
- 管理员点击“安排咨询”可指定来访者并创建预约。
- 创建预约会写入 `appointment` 表，状态为“静候确认”。
- 登录、修改个人资料、查看咨询历史、创建预约会写入 `audit_log` 表。

## 后端接口

- `GET /api/admin/audit-logs`
- `GET /api/admin/audit-logs/count`
- `GET /api/admin/audit-logs/{id}`
- `POST /api/appointments`

## 数据库

新增表：

- `audit_log`

更新脚本：

- `backend/src/main/resources/schema.sql`
- `backend/src/main/resources/data.sql`
- `sql/schema.sql`

真实 MySQL 可重新导入：

```bash
/opt/homebrew/opt/mysql@8.4/bin/mysql -u root < sql/schema.sql
```

## 验证

已验证：

- `mvn test`
- `npm run build`
- MySQL 导入 `sql/schema.sql`
- `GET /api/admin/audit-logs`
- `POST /api/appointments`
