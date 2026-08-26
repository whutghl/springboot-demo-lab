# Flyway 数据库迁移教学示例 — 使用说明

基于最简 `spdemo`（Spring Boot 2.7 + 单接口）改造而来，目标是理解 Flyway 的核心机制：**把数据库结构当成代码一样用 Git 管理、按版本号增量执行**。

技术栈：Spring Boot 2.7.18 + Flyway 8.5.13 + **MySQL 8** + MyBatis-Plus 3.5.3.1 + Lombok。

ORM 选型的教学意义：MyBatis-Plus **没有自动建表机制**，表结构完全由 Flyway 说了算，MyBatis-Plus 只负责 CRUD——职责分离更加天然。

---

## 一、启动后 curl 验证

```bash
# 1. 启动项目（首次启动会自动执行 db/migration 下所有脚本）
mvn spring-boot:run
```

启动日志中可以看到 Flyway 的输出（关键行）：

```text
INFO ... Flyway  | Database: jdbc:mysql://localhost:13306/flyway_demo (MySQL 8.0)
INFO ... Flyway  | Successfully validated 3 migrations
INFO ... Flyway  | Current version of schema: null
INFO ... Flyway  | Migrating schema "flyway_demo" to version "1 - create user table"
INFO ... Flyway  | Migrating schema "flyway_demo" to version "2 - add user phone"
INFO ... Flyway  | Migrating schema "flyway_demo" to version "3 - insert demo data"
INFO ... Flyway  | Successfully applied 3 migrations
```

然后用 curl 验证接口（表结构来自 Flyway，读写正常）：

```bash
# 查全部用户（V3 脚本预插的 2 条数据）
curl "http://localhost:10000/user/list"
# → {"code":200,"total":2,"users":[{"id":1,"name":"张三",...},{"id":2,"name":"李四",...}],...}

# 运行时新增用户（业务写入，与结构变更互不干扰）
curl "http://localhost:10000/user/add?name=王五&email=wangwu@example.com&phone=13800000003"
# → {"code":200,"message":"新增成功，id=3",...}

curl "http://localhost:10000/user/list"
# → total 变为 3
```

---

## 二、进阶实验：动手验证 Flyway 的核心机制

### 实验 1：改动已执行过的脚本 → checksum 校验失败（必做）

Flyway 会把每个脚本的**内容哈希（checksum）**记进 `flyway_schema_history` 表。下次启动时会重新计算并与记录对比，不一致直接拒绝启动——这是防止「同一张表被改了两次」的保险。

```bash
# 1. 把 V1__create_user_table.sql 里任意一行改一下（比如加一个空格或改个注释）
# 2. 重启应用，观察报错：
#    Flyway  | Validate failed: Migrations have failed validation
#    Migration checksum mismatch for migration version 1
#    -> Applied to database : 688865612
#    -> Resolved locally    : 123456789
```

**结论：旧脚本是历史，永远只追加、不修改。** 结构变更请写 V4、V5…

> 恢复方法：把改动还原，或删掉 `flyway_schema_history` 表里对应版本那行记录后再重启；如果库里的表和脚本已不一致，用 `flyway repair`（Maven 插件：`mvn flyway:repair`）修正 checksum 记录。

### 实验 2：新增 V4 脚本 → 体验增量迁移

新建 `src/main/resources/db/migration/V4__add_user_remark.sql`：

```sql
ALTER TABLE t_user ADD COLUMN remark VARCHAR(255) COMMENT '备注';
```

重启应用，日志会变成 `Migrating schema ... to version "4 - add user remark"`（只执行 V4，前面的不会重跑）。这模拟了真实发布流程：**上一个版本已经部署过的环境，升级时只执行新增的脚本**。

### 实验 3：查看迁移记录表

现在用的是真实 MySQL，直接连上去看 Flyway 的「记账本」 `flyway_schema_history` 表：

结果类似：

```text
+---------------+---------+---------------------+---------+------------+---------------------+
| installed_rank | version | description         | success | checksum   | installed_on        |
+---------------+---------+---------------------+---------+------------+---------------------+
|             1 | 1       | create user table   |       1 | 2142644839 | 2026-08-26 10:00:00 |
|             2 | 2       | add user phone      |       1 |  197987600 | 2026-08-26 10:00:01 |
|             3 | 3       | insert demo data    |       1 |  879845912 | 2026-08-26 10:00:01 |
+---------------+---------+---------------------+---------+------------+---------------------+
```

字段含义：

| 字段             | 含义                                                   |
| ---------------- | ------------------------------------------------------ |
| `version`      | 脚本版本号                                             |
| `description`  | 脚本描述                                               |
| `success`      | 是否执行成功（失败的迁移会标记为 false，不会污染历史） |
| `checksum`     | 脚本内容哈希（实验 1 靠它发现篡改）                    |
| `installed_on` | 执行时间                                               |

顺带一提：`t_user` 表此刻也在 `flyway_demo` 库里，`SHOW TABLES;` 就能看到——这就是「数据库结构由脚本管理」的直接证据。

### 实验 4：baseline — 接管家里的「老库」

`spring.flyway.baseline-on-migrate=true` 意味着：如果目标库已存在但没有 `flyway_schema_history` 表，Flyway 会打一个 baseline（版本 1 之前的锚点），然后继续执行比 baseline 新的脚本。生产上「给一个跑了 3 年的老库引入 Flyway」就是靠它，无需清空历史数据。

---

## 三、生产上一般怎么用

### 1. 一套脚本，多个环境

`db/migration` 随代码库一起走 Git，dev/qa/prod 共用**同一套**脚本；每个环境各有一张 `flyway_schema_history` 表当「记账本」，记录「这个环境已经跑到哪一版」。所以同一套代码在三个环境执行结果不同：新环境从 V1 跑到最新，老环境只补跑新增的脚本（实验 2 就是这个过程）。

升级发版 = 部署新代码 + 执行新增的迁移脚本，两者必须一起完成，否则就会出现「代码用了新字段、库里还没加列」的错位。

### 2. 迁移由谁执行：两种主流姿势

| 方式                                      | 做法                                                                    | 适用场景                                                                                        |
| ----------------------------------------- | ----------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------- |
| **A. 应用启动时自动迁移**（本项目） | 应用启动时 Flyway 自动扫描并执行未跑的脚本                              | 中小项目、单实例部署，最省事；多实例并发启动也没问题，Flyway 自带加锁，后来的实例会等先到的跑完 |
| **B. 发版流水线里显式执行**         | CI/CD 部署前用 Flyway 命令行 / Maven 插件先跑迁移，应用账号只做业务读写 | 大团队、强管控场景；迁移动作独立可审计，应用进程不再持有建表权限                                |

生产一般推荐 B：**迁移账号和应用账号分离**（迁移账号给 DDL + DML 权限，应用账号只给 DML），权限最小化，万一应用被攻破也动不了表结构。

### 3. 变更流程：只增不改、先加后删

- **已发版的脚本永远不动**——改了一个字节，checksum 对不上，所有环境全部启动失败（实验 1 就是这个保险）。新变更一律写 V4、V5…
- **破坏性变更要「先加后删」**：比如要删掉 `phone` 列，先 V4 加新列/新表 → 发版让代码切过去 → 过一两个版本再 V5 删旧列。这样滚动发布时，老实例还在用旧列也不会炸。
- **一个脚本只做一件事**，别把十张表的改动塞进一个脚本——中途失败排查和回滚都更简单。

### 4. 失败与回滚

- Flyway **不支持「撤销已执行的版本」**。回滚靠写**反向脚本**：V5 加了列不想要了，就写 V6 把它删掉，而不是回去改 V5。
- MySQL 的 DDL 会隐式提交、无法回滚，所以脚本要尽量幂等、小步快跑（详见下表第 4 点）。
- 脚本写错导致迁移失败：先把脚本改对，再 `mvn flyway:repair` 清掉失败记录，重新启动执行。

### 5. 生产配置建议

| 配置                                  | 建议值           | 原因                                                                    |
| ------------------------------------- | ---------------- | ----------------------------------------------------------------------- |
| `spring.flyway.clean-disabled`      | `true`         | `clean` 会清空整个库，生产必须禁用（默认值已经是 true，别改成 false） |
| `spring.flyway.out-of-order`        | 保持`false`    | 生产要求脚本严格按版本顺序执行，允许乱序容易掩盖分支混乱                |
| `spring.flyway.baseline-on-migrate` | 接老库时`true` | 给没有 Flyway 记录的老库打基线锚点，不丢历史数据（实验 4）              |
| 迁移账号权限                          | 只给本库         | 别用 root；迁移账号和应用账号分开                                       |

### 6. 监控：迁移状态怎么查

生产里没人看启动日志。常见做法：

- 直接查 `flyway_schema_history` 表：有没有 `success=0` 的记录；
- 把 `flyway.info()` 的版本号暴露成监控指标（如 Spring Boot Actuator + Micrometer），接入告警：**当前 schema 版本 < 代码期望版本 = 迁移没跑完，需要立刻处理**。

> 教学用的「启动自检」`CommandLineRunner`（打印迁移历史 + 表数据）已在本项目中移除——生产环境本来也不会写这种东西，看迁移状态用的是上面说的 `flyway_schema_history` 表 + 监控。想确认迁移效果，启动日志（见第一节）和 curl 就够了。

---

## 四、离生产级还差哪几步

| 关键点                               | 说明                                                                                                                                                                                                                                                                 |
| ------------------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **1. Flyway 版本与数据库匹配** | **MySQL 支持从 Flyway 8.2 起被拆到独立的 `flyway-mysql` 模块**（MySQL 驱动是 GPL 许可，Flyway 单独成模块规避传染），flyway-core + MySQL 必须显式加 flyway-mysql，否则报 `Unsupported Database: MySQL 8.0`；H2/PostgreSQL 则无需额外模块。本项目 pom 已配好 |
| **2. `clean` 是危险操作**    | `flyway clean` 会清空整个 schema（本项目就是 `flyway_demo` 库，数据全没）。生产环境要禁用它（`spring.flyway.clean-disabled=true`），只能在本机/测试环境用                                                                                                      |
| **3. 失败回滚策略**            | 注意：**MySQL 的 DDL（CREATE/ALTER/DROP）会隐式提交，不能回滚**，只有 DML 能在事务里回滚。所以脚本要「小步快跑」：一个脚本只做一件事、尽量幂等，避免中途失败后库里留下半套结构                                                                                 |
| **4. 多环境一致性**            | dev/qa/prod 各自维护一份`db/migration`（随代码库走），配合 CI 在部署前自动执行迁移，避免「开发能跑、生产建不了表」                                                                                                                                                 |
| **5. 迁移脚本的 code review**  | 迁移脚本和业务代码一样要走 review。破坏性变更（删列、改类型）要考虑兼容：先加列/加新表 → 发版 → 再删旧列，滚动发布才不会炸                                                                                                                                         |
| **6. 与测试的配合**            | 测试环境可用独立库或 Testcontainers（真实 MySQL 容器）跑迁移，保证测试环境的表结构和生产一致                                                                                                                                                                         |

---

## 五、核心原理（一图流）

```text
db/migration 目录                      flyway_schema_history 表
┌─────────────────────┐               ┌──────────────────────────────┐
│ V1__create_user_table.sql │────执行──▶│ version=1, checksum=xxx, SUCCESS │
│ V2__add_user_phone.sql   │────执行──▶│ version=2, checksum=yyy, SUCCESS │
│ V3__insert_demo_data.sql │────执行──▶│ version=3, checksum=zzz, SUCCESS │
│ V4__xxx.sql（未来）      │   ──待执行─▶│ （启动时对比后补执行）            │
└─────────────────────┘               └──────────────────────────────┘
```

1. **启动时**：Flyway 扫描 `db/migration`，按版本号升序排列；
2. **对比**：和 `flyway_schema_history` 表里已执行的记录对比——已执行且 checksum 一致 → 跳过；未执行 → 按顺序补执行；已执行但 checksum 不一致 → **拒绝启动**（实验 1）；
3. **执行**：每个脚本在事务中执行，成功后写入一条记录；
4. **结论**：数据库结构 = 一串有序的、可重放、可追溯的脚本集合，与代码一起走 Git、一起发版。这就是「数据库即代码（database as code）」。
