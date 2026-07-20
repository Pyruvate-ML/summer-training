# 心桥心理咨询平台后端原型

当前后端用于实训协作开发，采用 Spring Boot + H2 内存数据库，重点验证三类身份的数据权限，并为前端从静态数据迁移到真实接口提供最小可运行版本。

## 技术栈

- Java 17
- Spring Boot 3
- Spring Security
- Spring JDBC
- H2 Database
- Maven

## 运行

```bash
cd backend
mvn spring-boot:run
```

服务地址：

```text
http://localhost:8080
```

H2 控制台：

```text
http://localhost:8080/h2-console
```

H2 连接信息：

```text
JDBC URL: jdbc:h2:mem:xinqiao
User: sa
Password: 留空
```

## 测试账号

```text
管理员：admin / admin123
医生：doctor_zhang / doctor123
医生：doctor_lin / doctor123
就诊人员：patient_chen / patient123
就诊人员：patient_li / patient123
```

## 权限设计

管理员：

- 可查看所有预约。
- 可查看所有就诊人员档案。
- 可查看所有既往就诊单。
- 可访问 `/api/admin/**` 和 H2 控制台。

医生：

- 只能查看分配给自己的排班。
- 只能查看与自己有预约关系的病人档案。
- 只能查看自己负责病人的既往史和就诊单。
- 不能访问 `/api/admin/**` 和 H2 控制台。

就诊人员：

- 只能查看自己的预约。
- 只能查看自己的档案。
- 只能查看自己的既往就诊单。
- 不能查看其他病人和医生管理数据。

## 接口

```text
GET /api/health
GET /api/me
GET /api/appointments
GET /api/patients
GET /api/patients/{patientId}/history
GET /api/visit-records
GET /api/admin/dashboard
```

所有 `/api/**` 接口当前使用 HTTP Basic 认证。

示例：

```bash
curl -u admin:admin123 http://localhost:8080/api/appointments
curl -u doctor_zhang:doctor123 http://localhost:8080/api/appointments
curl -u patient_chen:patient123 http://localhost:8080/api/appointments
```

医生和就诊人员访问同一个接口时会得到不同范围的数据，便于前端联调时验证角色权限。

## 数据表

- `app_user`：登录用户和角色。
- `doctor`：医生/咨询师资料。
- `patient`：就诊人员档案。
- `appointment`：预约排班。
- `visit_record`：既往就诊单和咨询记录。

初始化脚本位于：

```text
src/main/resources/schema.sql
src/main/resources/data.sql
```

## 开发建议

- 新增表字段后同步更新 `schema.sql` 和 `data.sql`。
- 新增接口前先确认前端页面需要的字段，避免返回过多敏感信息。
- 当前 `ApiController` 是原型写法；接口稳定后建议拆分为 Controller、Service、Repository 和 DTO。
- 提交前执行：

```bash
mvn test
```
