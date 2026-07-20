# 心桥心理咨询平台

暑期实训协作项目。

心桥心理咨询平台是面向校园心理服务场景的协作管理系统，用于统一管理咨询预约、咨询师团队、来访者档案、评估等级和跟进计划。

当前仓库提供前端单页版本，并已整合 Spring Boot + H2 后端原型。前端用于确认页面流程，后端用于验证接口边界、数据库结构和管理员/医生/就诊人员三类角色权限，方便团队后续分模块开发。

## 当前功能

- 概览大厅：展示今日预约、静候确认、值班咨询师、重点关注者、今日待办预约和情绪指数趋势。
- 预约排期：支持预约列表展示、状态筛选和搜索。
- 咨询师团队：展示咨询师职称、擅长方向和当前负载。
- 来访者档案：展示来访主题、评估等级和跟进计划。
- 安排咨询：通过弹窗创建新的咨询预约。
- 登录识别：登录时只输入账号密码，不手动选择身份；后端根据账号识别管理员、医生、就诊人员。
- 后端权限：管理员、医生、就诊人员三类身份看到不同数据、菜单、首页卡片和功能入口。
- 角色工作台：管理员、医生、就诊人员登录后进入不同首页，看到不同导航、数据卡片和可访问功能。
- 个人资料：三类身份都有资料展示与编辑表单原型，包含身份信息、联系方式、权限说明和隐私提示。
- 后端接口：提供健康检查、登录识别、当前用户、预约、咨询师、来访者档案、既往记录、管理员用户和统计接口。

## 技术框架

当前版本：

- 前端：Vue 3 CDN、Tailwind CSS CDN、Chart.js CDN，单文件入口为 `index.html`。
- 后端：Java 17、Spring Boot 3、Spring Security、Spring JDBC、H2 Database，入口位于 `backend/`。
- 数据库：后端启动时自动执行 `backend/src/main/resources/schema.sql` 和 `backend/src/main/resources/data.sql`。
- 认证方式：当前原型提供 `POST /api/auth/login` 给前端登录页使用，同时保留 HTTP Basic 便于团队本地调试；后续可替换为 JWT 或 Session。
- 前端状态：登录页、身份工作台、导航和首页卡片已按后端登录响应渲染；部分列表与个人资料编辑仍是前端原型数据，后续可继续逐页接入接口。

后续建议迁移：

```text
frontend/
  src/
    layouts/AppLayout.vue
    views/Dashboard.vue
    views/Appointments.vue
    views/Counselors.vue
    views/Clients.vue
    components/StatusTag.vue
    components/EmotionTrendChart.vue
    api/
    stores/
backend/
  src/main/java/...
sql/
  schema.sql
docs/
  development.md
  roadmap.md
```

## 运行方式

前端：

```bash
python3 -m http.server 5175
```

访问：

```text
http://localhost:5175
```

后端：

```bash
cd backend
mvn spring-boot:run
```

访问：

```text
http://localhost:8080/api/health
```

后端接口需要认证，示例：

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

curl -u admin:admin123 http://localhost:8080/api/me
curl -u doctor_zhang:doctor123 http://localhost:8080/api/appointments
curl -u patient_chen:patient123 http://localhost:8080/api/patients
curl -u admin:admin123 http://localhost:8080/api/doctors
curl -u admin:admin123 http://localhost:8080/api/admin/users
```

后端默认账号：

```text
管理员：admin / admin123
医生：doctor_zhang / doctor123
就诊人员：patient_chen / patient123
```

登录成功后，后端会返回当前用户、中文身份名、身份工作台标题、可访问导航、首页卡片和权限清单。前端不要写死“当前是什么身份”，直接按照接口返回渲染。

## 目录说明

```text
.
├── index.html          # 当前前端入口，包含 Vue/Tailwind/Chart.js 单页实现
├── backend/            # Spring Boot + H2 后端原型
├── favicon.svg         # 浏览器图标
├── sql/schema.sql      # 数据库建表与基础数据
├── docs/development.md # 协作开发说明
├── docs/backend-integration.md # 后端整合记录和联调说明
├── docs/roadmap.md     # 后续功能扩展规划
└── README.md
```

## 协作约定

- 页面文案必须保持心理咨询业务语境，避免使用具有攻击性或标签化的词。
- 预约、咨询师、来访者等核心数据结构先参考 `sql/schema.sql`。
- 新增功能时优先保持模块边界清晰，例如预约排期、来访档案、咨询记录、测评记录分别独立。
- 提交代码前请确认页面可以在本地正常打开，并检查控制台没有功能性报错。
- 后端提交前请在 `backend/` 目录执行 `mvn test`。
- 前后端联调时先固定接口响应字段，再替换前端静态数组，避免页面和接口同时大改。

## 后续扩展方向

- 将前端静态数据逐步接入后端接口。
- 将个人资料编辑表单接入后端保存接口。
- 把 HTTP Basic 替换为团队最终约定的认证方案。
- 增加咨询记录和跟进记录。
- 增加心理测评记录和情绪趋势统计。
- 增加重点关注提醒和处理状态流转。
- 将当前单文件前端迁移为 Vue 3 + Vite 项目。
