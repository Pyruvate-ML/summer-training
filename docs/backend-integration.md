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

医生：

- 只能查看分配给自己的预约。
- 只能查看与自己存在预约关系的来访者档案。
- 只能查看自己负责来访者的既往记录。

就诊人员：

- 只能查看自己的预约。
- 只能查看自己的档案。
- 只能查看自己的既往记录。

## 接口清单

```text
GET /api/health
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
