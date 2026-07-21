# 操作日志与预约咨询闭环更新说明

## 本次更新内容

本次完成了“操作日志与审计追踪”模块，并补齐病人端“预约咨询”按钮的真实提交闭环。

主要效果：

- 管理员登录后可在左侧“审计日志”页面查看操作记录。
- 病人登录后可进入“预约咨询”页面提交预约申请。
- 预约提交后写入 MySQL 的 `appointment` 表。
- 预约提交同时写入 `audit_log` 表，记录操作人、操作时间、操作对象、操作类型、提交内容和原因。

## 技术栈与框架

本次改动沿用项目现有技术栈：

- 后端：Java 17、Spring Boot 3、Spring Security、Spring JDBC、MySQL
- 前端：Vue 3、Tailwind CSS、像素风静态页面
- 数据库：MySQL，开发库名 `xinqiao_counseling`

没有新增前后端框架，也没有引入新的构建工具。

## 后端改动

新增文件：

- `backend/src/main/java/com/xinqiao/counseling/AuditLogService.java`

修改文件：

- `backend/src/main/java/com/xinqiao/counseling/ApiController.java`

新增接口：

- `GET /api/admin/audit-logs`
- `GET /api/admin/audit-logs/count`
- `GET /api/admin/audit-logs/{id}`
- `POST /api/appointments`

已接入审计的操作：

- 登录：`LOGIN`
- 查看来访者咨询历史：`VIEW_SENSITIVE`
- 修改个人资料：`UPDATE_PROFILE`
- 病人提交预约申请：`CREATE_APPOINTMENT`

## 数据库改动

修改文件：

- `backend/src/main/resources/schema.sql`
- `backend/src/main/resources/data.sql`
- `sql/schema.sql`

新增表：

- `audit_log`

核心字段：

- `operator_id`
- `operator_name`
- `operation_type`
- `target_type`
- `target_id`
- `target_description`
- `old_value`
- `new_value`
- `reason`
- `ip_address`
- `created_at`

## 前端改动

修改文件：

- `frontend/index.html`
- `frontend/src/App.vue`

主要功能：

- 管理员侧边栏新增“审计日志”入口。
- 审计日志页面展示操作时间、操作人、操作类型、操作对象、操作原因。
- 病人端“预约咨询”页面新增预约申请表单。
- 提交成功后刷新“我的预约”列表。

## 演示方式

1. 启动后端：

```powershell
cd backend
$env:XINQIAO_DB_PASSWORD="本地 MySQL 密码"
mvn spring-boot:run
```

2. 启动前端：

```powershell
cd frontend
npm run dev
```

3. 病人端演示：

- 登录：`patient_chen / patient123`
- 点击左侧“预约咨询”
- 填写预约主题、时间、地点和原因
- 提交后进入“我的预约”，可看到新预约状态为“静候确认”

4. 管理员端演示：

- 登录：`admin / admin123`
- 点击左侧“审计日志”
- 可看到病人提交预约申请的 `CREATE_APPOINTMENT` 日志

## 注意事项

- 本次提交在功能分支完成，未直接提交到 `main`。
- 数据库需使用最新 SQL，确保存在 `audit_log` 表。
- 本地数据库密码请通过 `XINQIAO_DB_PASSWORD` 环境变量配置，不要提交个人密码。
