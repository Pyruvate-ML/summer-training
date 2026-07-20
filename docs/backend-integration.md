# 后端整合记录

## GitHub 提交检查

本次检查远端仓库 `Pyruvate-ML/summer-training`，发现 `origin/day1` 分支包含后端相关提交：

```text
26a4c83 开工
1c08285 Merge branch 'main' into day1
```

该分支新增了 Spring Boot 后端、H2 数据库、用户/医生/预约等表结构，以及登录、注册、角色判断等后端代码。因为 `main` 当前是前端原型项目，如果直接把后端 Maven 文件放在仓库根目录，会和前端入口、静态资源、文档结构混在一起，所以整合时采用 `backend/` 子目录作为统一后端模块入口。

## 当前整合结果

后端模块位于：

```text
backend/
  pom.xml
  src/main/java/com/xinqiao/counseling/
    CounselingBackendApplication.java
    SecurityConfig.java
    ApiController.java
  src/main/resources/
    application.yml
    schema.sql
    data.sql
```

当前后端以可运行、易联调为优先目标：

- 使用 Spring Boot Web 暴露 REST API。
- 使用 Spring Security 提供管理员、医生、就诊人员三类账号。
- 新增登录接口，前端只提交账号密码，后端按账号识别角色并返回对应工作台配置。
- 使用 Spring JDBC 直接查询 H2 数据库，减少实训早期的 Mapper 和实体维护成本。
- 使用 H2 内存库，每次启动自动初始化表结构和演示数据。
- 保留和前端原型一致的业务字段，包括预约状态、评估等级、跟进计划和既往就诊记录。

## 技术栈

- Java 17
- Spring Boot 3.3
- Spring Security
- Spring JDBC
- H2 Database
- Maven

## 权限思路

管理员：

- 可以访问 `/api/admin/**`。
- 可以查看全部预约、来访者档案和既往记录。
- 可以访问 H2 控制台查看本地数据。
- 首页显示全局运营卡片，包括预约、咨询师、来访者、系统用户和系统健康。
- 导航包含概览大厅、预约管理、咨询师管理、来访者档案、咨询记录、系统用户、数据统计。

医生：

- 只能查看分配给自己的预约。
- 只能查看与自己存在预约关系的来访者档案。
- 只能查看自己负责来访者的既往记录。
- 首页显示我的预约、待填写记录、重点关注来访者和下一次跟进。
- 导航包含我的预约、我的来访者、咨询记录、跟进计划、个人资料。

就诊人员：

- 只能查看自己的预约。
- 只能查看自己的档案。
- 只能查看自己的既往记录。
- 首页显示我的预约、下一次预约、跟进建议和咨询记录数量。
- 导航包含我的预约、预约咨询、我的咨询记录、跟进计划、个人资料。

## 登录响应契约

前端登录页调用：

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

后端根据账号密码查询 `app_user`，登录成功后返回：

```text
token
authType
user.id
user.username
user.displayName
user.role
user.roleName
workspace.title
workspace.subtitle
workspace.primaryAction
navigation[]
dashboardCards[]
permissions.canView[]
permissions.canManage[]
```

前端不要主动让用户选择身份，也不要在页面写死身份菜单。不同身份主页、导航和选项都应由登录响应决定。

## 接口清单

```text
GET /api/health
POST /api/auth/login
GET /api/me
GET /api/appointments
GET /api/patients
GET /api/patients/{patientId}/history
GET /api/visit-records
GET /api/admin/dashboard
```

调试示例：

```bash
cd backend
mvn spring-boot:run

curl -u admin:admin123 http://localhost:8080/api/admin/dashboard
curl -u doctor_zhang:doctor123 http://localhost:8080/api/appointments
curl -u patient_chen:patient123 http://localhost:8080/api/visit-records
```

## 后续开发建议

1. 前端先从 `/api/appointments`、`/api/patients` 两个只读接口开始接入，确认字段名称后再继续接入新增和状态修改。
2. 后端新增写接口时优先保持小粒度，例如 `POST /api/appointments`、`PUT /api/appointments/{id}/status`。
3. 如果团队决定采用 JWT，可以参考 `origin/day1` 中登录、注册、拦截器和角色注解的实现，再替换当前 HTTP Basic 原型。
4. 如果后端表结构变化，同步更新 `backend/src/main/resources/schema.sql`、`data.sql` 和本文档接口说明。
5. 不提交 `target/`、`.idea/`、H2 文件数据库等本地生成文件。
