# MySQL / schema.sql 协作方案

更新时间：2026-07-22

## 当前数据库定位

项目后端已经切到 MySQL。`backend/src/main/resources/schema.sql` 是团队共享的表结构来源，`data.sql` 是演示数据来源。

本次 RBAC 与辅导员联动新增表：

- `rbac_role`：角色字典，包含管理员、咨询师、辅导员、来访者。
- `permission`：权限字典。
- `role_permission`：角色-权限矩阵。
- `counselor`：辅导员资料。
- `patient_counselor`：学生与辅导员绑定关系。

## 有 MySQL 的同学

首次或允许重置演示数据时：

```bash
mysql -uroot -p -e "DROP DATABASE IF EXISTS xinqiao_counseling; CREATE DATABASE xinqiao_counseling CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -uroot -p xinqiao_counseling < backend/src/main/resources/schema.sql
mysql -uroot -p xinqiao_counseling < backend/src/main/resources/data.sql
```

不想重置数据时，不要直接执行当前 `schema.sql`，因为它包含 `DROP TABLE`。需要单独写迁移 SQL。

## 暂时无法安装 MySQL 的同学

推荐用 Docker 跑 MySQL，不需要在系统里安装 MySQL 服务：

```bash
docker run --name xinqiao-mysql -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=xinqiao_counseling -p 3306:3306 -d mysql:8.4
```

然后执行同样的导入命令：

```bash
mysql -h127.0.0.1 -uroot -proot xinqiao_counseling < backend/src/main/resources/schema.sql
mysql -h127.0.0.1 -uroot -proot xinqiao_counseling < backend/src/main/resources/data.sql
```

如果 Docker 也不可用，可以先不启动后端，只维护 `schema.sql` 和 `data.sql`，通过 PR 让有 MySQL 的同学执行验证。

## Spring Boot 配置建议

开发时建议使用环境变量，不把密码写死到仓库：

```bash
XINQIAO_DB_URL=jdbc:mysql://localhost:3306/xinqiao_counseling?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
XINQIAO_DB_USERNAME=root
XINQIAO_DB_PASSWORD=你的密码
XINQIAO_SQL_INIT_MODE=never
```

只有需要重置演示数据时，才把 `XINQIAO_SQL_INIT_MODE` 临时设为 `always`。
