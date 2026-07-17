# 心桥心理咨询平台

暑期实训协作项目。

心桥心理咨询平台是面向校园心理服务场景的协作管理系统，用于统一管理咨询预约、咨询师团队、来访者档案、评估等级和跟进计划。

当前仓库先提供前端单页版本，便于团队协作确认业务流程和界面结构。后续可以在此基础上接入后端接口、数据库和权限模块。

## 当前功能

- 概览大厅：展示今日预约、静候确认、值班咨询师、重点关注者、今日待办预约和情绪指数趋势。
- 预约排期：支持预约列表展示、状态筛选和搜索。
- 咨询师团队：展示咨询师职称、擅长方向和当前负载。
- 来访者档案：展示来访主题、评估等级和跟进计划。
- 安排咨询：通过弹窗创建新的咨询预约。

## 技术框架

当前版本：

- Vue 3 CDN：负责页面状态、列表渲染、筛选和弹窗交互。
- Tailwind CSS CDN：负责页面布局和样式。
- Chart.js CDN：负责情绪指数趋势图。
- 单文件入口：`index.html`。

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

在项目目录启动静态服务：

```bash
python3 -m http.server 5175
```

访问：

```text
http://localhost:5175
```

## 目录说明

```text
.
├── index.html          # 当前前端入口，包含 Vue/Tailwind/Chart.js 单页实现
├── favicon.svg         # 浏览器图标
├── sql/schema.sql      # 数据库建表与基础数据
├── docs/development.md # 协作开发说明
├── docs/roadmap.md     # 后续功能扩展规划
└── README.md
```

## 协作约定

- 页面文案必须保持心理咨询业务语境，避免使用具有攻击性或标签化的词。
- 预约、咨询师、来访者等核心数据结构先参考 `sql/schema.sql`。
- 新增功能时优先保持模块边界清晰，例如预约排期、来访档案、咨询记录、测评记录分别独立。
- 提交代码前请确认页面可以在本地正常打开，并检查控制台没有功能性报错。

## 后续扩展方向

- 接入登录和角色权限。
- 接入预约排班后端接口。
- 增加咨询记录和跟进记录。
- 增加心理测评记录和情绪趋势统计。
- 增加重点关注提醒和处理状态流转。
- 将当前单文件前端迁移为 Vue 3 + Vite 项目。
