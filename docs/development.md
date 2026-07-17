# 协作开发说明

## 当前阶段

当前项目处于前端原型和业务建模阶段，主要目标是明确页面结构、业务模块、数据字段和后续接口边界。

## 前端模块边界

- 概览大厅：聚合统计、今日待办、情绪趋势。
- 预约排期：预约列表、状态筛选、新增预约。
- 咨询师团队：咨询师信息、擅长方向、当前负载。
- 来访者档案：来访主题、评估等级、跟进计划。

## 后端接口建议

```text
GET    /api/dashboard/summary
GET    /api/appointments
POST   /api/appointments
PUT    /api/appointments/{id}/status
GET    /api/counselors
GET    /api/clients
GET    /api/clients/{id}
POST   /api/clients/{id}/follow-ups
GET    /api/statistics/emotion-trend
```

## Git 协作建议

- `main`：稳定分支。
- `feature/appointments`：预约排期功能。
- `feature/clients`：来访者档案功能。
- `feature/counselors`：咨询师团队功能。
- `feature/backend-api`：后端接口功能。

提交信息建议：

```text
feat: add appointment scheduling page
fix: correct client risk label
docs: update database design
style: refine dashboard layout
```

## 数据约定

- 预约状态：`静候确认`、`已排定`、`咨询已结束`。
- 评估等级：`普通`、`关注`、`重点`。
- 删除数据建议使用逻辑删除字段，不直接物理删除。

## 安全和隐私

- 来访者真实姓名、联系方式、咨询记录属于敏感数据。
- 后续接入后端时，咨询记录应限制访问权限。
- 页面展示时避免使用贬低性词汇，例如“危险人员”“重点监控”。
