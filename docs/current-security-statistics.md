# 当前统计与安全隐私实现说明

更新时间：2026-07-22

## 认证与注册

- `POST /api/auth/login`：公开接口，登录成功后返回角色、导航、工作台卡片和固定权限说明。
- `POST /api/auth/register`：公开接口，只允许自助注册 `PATIENT` 或 `DOCTOR`，不允许自助注册管理员。
- 当前仍使用 HTTP Basic 作为演示认证方案；后端已改为返回普通 JSON 401，不再触发浏览器原生登录弹窗。

## 管理员统计

`GET /api/admin/statistics?range=day|week|month`

- `overview`：系统总量、周期预约数、周期咨询记录数、活跃来访者、活跃咨询师。
- `appointmentStatusDistribution`：按预约状态统计。
- `doctorWorkload`：每位咨询师的预约数、咨询记录数、关联来访者数。
- `patientActivity`：每位来访者的预约数、咨询记录数、接触咨询师数。
- `doctorPatientPairs`：咨询师与来访者组合维度下的预约/咨询次数。
- `assessmentLevelDistribution`：按关注等级统计。
- `operationSignals`：周期内敏感查看、资料更新、预约创建次数。

## 隐私与脱敏边界

- `/api/patients` 不再返回 `SELECT *`，按角色返回最小字段集合。
- 咨询师只能看到与自己存在预约关系的来访者必要字段。
- `/api/visit-records` 只返回摘要和下一步计划，不返回 `treatment_note`。
- `/api/patients/{patientId}/history` 属于敏感详情接口，必须传非空 `reason`，并通过角色权限校验，访问会写审计日志和 IP。
- 当前脱敏是“接口返回字段最小化 + 权限校验 + 审计”的应用层方案，不等同于数据库字段加密；后续若进入真实生产环境，应补 JWT/Session、HTTPS、数据库账号最小权限、字段加密/哈希和日志留存策略。
