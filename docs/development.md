# 协作开发说明

## 当前阶段

当前项目处于前端原型、后端原型和接口联调准备阶段，主要目标是明确页面结构、业务模块、数据字段、角色权限和接口边界。

## 前端模块边界

- 概览大厅：聚合统计、今日待办、情绪趋势。
- 预约排期：预约列表、状态筛选、新增预约。
- 咨询师团队：咨询师信息、擅长方向、当前负载。
- 来访者档案：来访主题、评估等级、跟进计划。

## 后端接口建议

```text
GET    /api/health
GET    /api/me
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

当前已落地的后端接口见 `docs/backend-integration.md`。还没有落地的接口先作为后续任务拆分，不要一次性塞进一个大提交。

## 前后端联调流程

1. 后端先在 `backend/src/main/resources/data.sql` 中准备稳定演示数据。
2. 前端保留当前静态数据结构，新增接口适配函数，不直接在页面里写 `fetch` 细节。
3. 每次只替换一个模块的数据来源，例如先替换预约列表，再替换来访者档案。
4. 字段名变化时，优先在接口适配层兼容，确认稳定后再清理旧字段。
5. 涉及咨询记录、评估等级、既往史等敏感信息时，先确认角色权限再开放页面入口。

## 后端模块边界

- `SecurityConfig`：认证方式、角色访问规则、跨域配置和数据库用户加载。
- `ApiController`：当前原型阶段集中放置只读接口，便于团队快速理解接口返回。
- `schema.sql`：表结构，保持字段含义清楚。
- `data.sql`：演示账号和业务数据，保证本地启动即可联调。

后续接口增多后，可以再拆分为：

```text
controller/
service/
repository/
dto/
config/
```

## Git 协作建议

- `main`：稳定分支。
- `feature/appointments`：预约排期功能。
- `feature/clients`：来访者档案功能。
- `feature/counselors`：咨询师团队功能。
- `feature/backend-api`：后端接口功能。
- `feature/backend-auth`：登录、认证和角色权限功能。

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
- 后端返回给前端的字段尽量使用稳定英文名，页面展示再转换成中文文案。

## 安全和隐私

- 来访者真实姓名、联系方式、咨询记录属于敏感数据。
- 后续接入后端时，咨询记录应限制访问权限。
- 页面展示时避免使用贬低性词汇，例如“危险人员”“重点监控”。
