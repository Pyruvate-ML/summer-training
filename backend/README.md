# 心桥心理咨询平台后端原型

当前后端用于实训协作开发，采用 Spring Boot + MySQL，重点验证三类身份的数据权限，并为 Vue 3 + Vite 前端提供最小可运行接口。

## 技术栈

- Java 17
- Spring Boot 3
- Spring Security
- Spring JDBC
- MySQL
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

MySQL 连接信息：

```text
Database: xinqiao_counseling
User: root
```

本地启动前设置数据库密码环境变量，避免把个人密码提交到仓库：

```powershell
$env:XINQIAO_DB_PASSWORD="你的本地 MySQL 密码"
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
- 可访问 `/api/admin/**`。

医生：

- 只能查看分配给自己的排班。
- 只能查看与自己有预约关系的病人档案。
- 只能查看自己负责病人的既往史和就诊单。
- 不能访问 `/api/admin/**`。

就诊人员：

- 只能查看自己的预约。
- 只能查看自己的档案。
- 只能查看自己的既往就诊单。
- 不能查看其他病人和医生管理数据。

## 登录和身份识别

前端登录页只提交账号和密码，不提供身份选择框。后端通过 `app_user.role` 自动识别身份。

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"doctor_zhang","password":"doctor123"}'
```

登录成功返回：

```text
token           原型阶段 token，占位给前端保存登录态
user            当前用户、账号、显示名、角色和中文身份名
workspace       当前身份的首页标题、副标题和主操作
navigation      当前身份可见的导航菜单
dashboardCards  当前身份首页卡片
permissions     当前身份可访问和可管理的功能清单
```

前端约定：

- 登录页不要让用户勾选身份。
- 登录成功后以 `user.role`、`navigation` 和 `dashboardCards` 渲染首页。
- 退出登录时清除本地保存的登录响应，回到登录页。
- 当前 `token` 是原型占位，不用于真实鉴权；接口调试和 Vite 前端列表请求暂时使用 HTTP Basic。

## 接口

```text
GET /api/health
POST /api/auth/login
GET /api/me
GET /api/appointments
GET /api/doctors
GET /api/patients
GET /api/patients/{patientId}/history
GET /api/visit-records
GET /api/admin/dashboard
GET /api/admin/users
```

`POST /api/auth/login` 和 `GET /api/health` 不需要认证。其他 `/api/**` 接口当前使用 HTTP Basic 认证，认证账号统一读取 MySQL `app_user` 表。

示例：

```bash
curl -u admin:admin123 http://localhost:8080/api/appointments
curl -u doctor_zhang:doctor123 http://localhost:8080/api/appointments
curl -u patient_chen:patient123 http://localhost:8080/api/appointments
curl -u admin:admin123 http://localhost:8080/api/doctors
curl -u admin:admin123 http://localhost:8080/api/admin/users
```

医生和就诊人员访问同一个接口时会得到不同范围的数据，便于前端联调时验证角色权限。

接口权限说明：

- `/api/appointments`：管理员看全部，医生看分配给自己的，就诊人员看自己的。
- `/api/patients`：管理员看全部，医生看有咨询关系的，就诊人员看自己的。
- `/api/doctors`：管理员看全部，医生看自己，就诊人员看与自己有预约关系的咨询师。
- `/api/visit-records`：管理员看全部，医生看自己负责的，就诊人员看自己的。
- `/api/admin/users`：仅管理员查看系统用户列表。

## 数据表

- `app_user`：登录用户、密码和角色。
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
- 正式项目不要明文保存密码，后续需要替换为 BCrypt 等安全哈希。
- 提交前执行：

```bash
mvn test
```
