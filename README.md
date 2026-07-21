# 心桥心理咨询平台

暑期实训协作项目。

心桥心理咨询平台是面向校园心理服务场景的协作管理系统，用于统一管理咨询预约、咨询师团队、来访者档案、评估等级和跟进计划。

当前仓库已进入前后端分离改造阶段：`frontend/` 提供 Vue 3 + Vite 前端工程，`backend/` 提供 Spring Boot + MySQL 后端原型。根目录 `index.html` 保留为早期静态原型参考。

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

- 前端：Vue 3、Vite，入口位于 `frontend/`，像素风素材复制到 `frontend/public/assets/`。
- 后端：Java 17、Spring Boot 3、Spring Security、Spring JDBC、MySQL，入口位于 `backend/`。
- 数据库：开发库为 `xinqiao_counseling`，包含用户、个人资料、咨询师、来访者、预约、病例和站内信 7 张表；后端启动时自动执行 `backend/src/main/resources/schema.sql` 和 `backend/src/main/resources/data.sql`。
- 认证方式：登录接口和 HTTP Basic 调试认证统一读取 MySQL `app_user` 表；后续正式版可替换为 JWT 或 Session。
- 前端状态：登录、角色工作台、导航、首页卡片和主要列表已迁入 Vite 工程；根目录 `index.html` 暂时作为旧原型留存。

## 运行方式

前端工程：

```bash
cd frontend
npm install
npm run dev
```

访问：

```text
http://127.0.0.1:5173
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

MySQL 开发库：

```text
Database: xinqiao_counseling
User: root
```

首次初始化也可以使用 MySQL Workbench：打开 `Server -> Data Import`，选择
`Import from Self-Contained File`，导入仓库中的 `sql/schema.sql`。该脚本会创建
`xinqiao_counseling` 数据库、全部 7 张业务表及联调用的基础数据。

收到的 `xinqiao_counseling.sql` 已按当前项目接口完成兼容整合：登录密码改为
BCrypt 哈希，并补充个人资料、站内信、时间字段和必要索引，避免导入旧脚本后
现有登录及信箱功能失效。

本地启动前设置数据库密码环境变量，避免把个人密码提交到仓库：

```powershell
$env:XINQIAO_DB_PASSWORD="你的本地 MySQL 密码"
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
├── frontend/           # Vue 3 + Vite 前端工程
├── backend/            # Spring Boot + MySQL 后端原型
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

- 继续补全前端 CRUD 表单和接口联调。
- 将 `frontend/` 中更多页面接入 `/api/profile`、预约写入和状态流转接口。
- 把 HTTP Basic 替换为团队最终约定的认证方案。
- 增加咨询记录和跟进记录。
- 增加心理测评记录和情绪趋势统计。
- 增加重点关注提醒和处理状态流转。
