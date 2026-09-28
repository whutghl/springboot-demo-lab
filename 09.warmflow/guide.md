# Warm-Flow 工作流引擎教学示例 — 使用说明

基于最简 `spdemo`（Spring Boot 2.7）改造而来，目标是理解 **Warm-Flow** 这个国产轻量级工作流引擎的核心机制：**把「审批流程」定义成一份可导入、可发布、可多次实例化的流程文件，业务系统通过几个引擎 API 驱动流程流转。**

技术栈：Spring Boot 2.7.18 + **Warm-Flow 1.3.3**（`warm-flow-mybatis-plus-sb-starter`）+ **MySQL 8** + MyBatis-Plus 3.5.3.1 + Lombok。

教学案例：**请假审批**（组长审批 → 经理审批），覆盖发起、通过、退回、查待办、查历史五个核心动作。

---

## 零、环境准备

1. **JDK 8+ / Maven 3.6+**
2. **MySQL 8**（本案例连接 `localhost:3306`，账号 `root/123`，可在 `src/main/resources/application-dev.yml` 里改）

> **数据库配置说明**：数据库连接（账号密码）放在 `application-dev.yml`，该文件已加入 `.gitignore`（不提交版本库）。如果你 clone 项目后发现没有这个文件，需自行创建一份，内容参考 `application.yml` 中注释或项目里的 `application-dev.yml` 模板。

---

## 一、初始化数据库

Warm-Flow 引擎需要 **7 张表**（`flow_definition` / `flow_node` / `flow_skip` / `flow_instance` / `flow_task` / `flow_his_task` / `flow_user`），外加业务请假表 `t_leave`。引擎表结构由官方定义，**不会自动建表**，需要你手动执行一次建表脚本：

```bash
mysql -uroot -p < src/main/resources/db/init.sql
```

脚本会：

- 创建数据库 `warm_flow_demo`
- 创建 7 张引擎表 + 1 张业务表

> 这一步只做一次。之后每次启动应用，引擎直接复用这些表。

---

## 二、启动应用

```bash
mvn spring-boot:run
```

启动后**不会**自动导入流程定义，需要手动初始化一次（二选一）：

```bash
# 方式一：curl 调用显式接口（「导入 → 发布」两步都发生在这次调用里）
curl "http://localhost:10000/leave/init"
# → data.step1 导入：defService.importXml(db/leaveFlow.xml) 成功，生成 definitionId=xxx
# → data.step2 发布：defService.publish(xxx) 成功，可通过 /leave/start 发起请假
```

方式二：打开验证台页面 `http://localhost:10000/index.html`，点左上角「⓪ 导入并发布流程定义」按钮。

> **刻意不做启动自动导入**，而是把「导入 → 发布」暴露成显式接口 `/leave/init`，让你亲眼看到 `importXml` 与 `publish` 两个引擎 API 的调用过程。**只有"已发布"的流程才能启动实例**，这就是导入/发布两个概念的教学点。接口幂等：已存在且已发布时再次调用会直接跳过。

---

## 三、curl 验证完整流转

### 0. 初始化流程定义（首次使用才需要）

```bash
curl "http://localhost:10000/leave/init"
# → {"code":200,"message":"流程定义导入并发布成功","data":{"step1":"导入：...","step2":"发布：...","definitionId":xxx}}
```

> 幂等：再次调用会返回「已存在且已发布，无需重复初始化」。

### 1. 发起请假（启动流程实例）

```bash
#curl "http://localhost:10000/leave/start?applicant=张三&days=3&reason=家中有事"

curl -G "http://localhost:10000/leave/start" \
  --data-urlencode "applicant=张三" \
  --data-urlencode "days=3" \
  --data-urlencode "reason=家中有事"

# → {"code":200,"message":"发起请假成功，流程已进入「组长审批」节点","data":{...,"instanceId":xxx,"nodeName":"组长审批",...}}
```

### 2. 查看请假单列表（业务表当前进度）

```bash
curl "http://localhost:10000/leave/list"
# → 张三的请假单，nodeName=组长审批，flowStatus=1（审批中）
```

### 3. 查待办任务（引擎表 flow_task）

```bash
curl "http://localhost:10000/leave/todo?instanceId=xxx"
# → tasks 里有一条 nodeName=组长审批 的待办，记录其 task.id
```

### 4. 组长审批通过 → 流转到经理审批

```bash
curl "http://localhost:10000/leave/approve?leaveId=1&pass=true&operatorId=组长&permissionFlag=role:leader&message=同意"
# → 当前节点变为「经理审批」
```

### 5. 经理审批通过 → 流程结束

```bash
curl "http://localhost:10000/leave/approve?leaveId=1&pass=true&operatorId=经理&permissionFlag=role:manager&message=同意"
# → 当前节点变为「结束」，flowStatus=8（已完成）
```

### 6. 查审批历史（引擎表 flow_his_task）

```bash
curl "http://localhost:10000/leave/history?instanceId=xxx"
# → 两条历史：组长、经理的审批记录（含 skip_type、message）
```

### 7. 演示退回

> 注意：本流程拓扑里**组长节点不支持退回**（引擎禁止退回到开始节点），退回请用**经理节点**（经理退回后单据回到组长，可再审批）。

```bash
# 重新发起一条请假
curl "http://localhost:10000/leave/start?applicant=李四&days=1&reason=年假"
# 先组长通过
curl "http://localhost:10000/leave/approve?leaveId=2&pass=true&operatorId=组长&permissionFlag=role:leader&message=同意"
# 经理退回（pass=false）→ 单据回到组长节点
curl "http://localhost:10000/leave/approve?leaveId=2&pass=false&operatorId=经理&permissionFlag=role:manager&message=证据不足，退回"
# → 当前节点变回「组长审批」，flowStatus=9（已退回）
# 组长再审通过 → 经理再通过 → 结束
curl "http://localhost:10000/leave/approve?leaveId=2&pass=true&operatorId=组长&permissionFlag=role:leader&message=补充材料已提交"
curl "http://localhost:10000/leave/approve?leaveId=2&pass=true&operatorId=经理&permissionFlag=role:manager&message=同意"
# → flowStatus=8（已完成）
```

---

## 四、核心概念：把"流程"当配置管理

Warm-Flow 最核心的心智模型：**流程定义 ≠ 流程实例**。

| 概念               | 对应表                                                | 说明                                                                   |
| ------------------ | ----------------------------------------------------- | ---------------------------------------------------------------------- |
| **流程定义** | `flow_definition` / `flow_node` / `flow_skip`   | 一份"图纸"，描述流程有哪些节点、怎么跳转。可导入、可发布、可升级版本   |
| **流程实例** | `flow_instance` / `flow_task` / `flow_his_task` | 基于图纸跑起来的一次具体业务（一次请假就是一条实例），实例之间互不影响 |
| **流程用户** | `flow_user`                                         | 每个待办任务关联的办理人权限                                           |

流程定义文件 `db/leaveFlow.xml` 长这样（结构见文件内注释）：

```xml
<definition flowCode="leaveFlow" flowName="请假审批流程" version="1">
    <node nodeType="start"   nodeCode="start" nodeName="开始">
        <skip skipName="发起" skipType="PASS">leaderAudit</skip>
    </node>
    <node nodeType="between" nodeCode="leaderAudit" nodeName="组长审批" permissionFlag="role:leader">
        <skip skipName="通过" skipType="PASS">managerAudit</skip>
    </node>
    <node nodeType="between" nodeCode="managerAudit" nodeName="经理审批" permissionFlag="role:manager">
        <skip skipName="通过" skipType="PASS">end</skip>
        <skip skipName="退回" skipType="REJECT">leaderAudit</skip>
    </node>
    <node nodeType="end" nodeCode="end" nodeName="结束"/>
</definition>
```

- 根节点 `flowCode`：流程编码，启动实例时用它指定"跑哪份图纸"
- `node` 的 `nodeType`：`start` 开始 / `between` 中间 / `end` 结束 / `serial` 互斥网关 / `parallel` 并行网关
- `node` 的 `permissionFlag`：办理人权限标识（如 `role:leader`），审批时通过 `FlowParams.permissionFlag` 传入办理人权限做校验
- `skip` 的 `skipType`：`PASS` 通过 / `REJECT` 退回；`skip` 的**元素文本** = 下一个节点编码
- `start` 开始节点**也必须有 `skip`**（引擎要求开始/中间节点必须有跳转规则）；`between` 中间节点可配多条 `skip`（如经理节点既有 PASS 又有 REJECT）

---

## 五、核心 API（业务代码只碰 3 个 Service）

Warm-Flow 引入 starter 后自动注册引擎 Bean，业务代码直接注入即可（本项目见 `LeaveService`）。

| 动作             | 引擎 API                                     | 说明                                             |
| ---------------- | -------------------------------------------- | ------------------------------------------------ |
| 导入流程         | `defService.importXml(InputStream)`        | 把`leaveFlow.xml` 解析成流程定义并落库         |
| 发布流程         | `defService.publish(defId)`                | 发布后才能启动实例                               |
| 发起（启动实例） | `insService.start(businessId, flowParams)` | 第一个参数是**业务 id**，返回 `Instance` |
| 审批通过/退回    | `taskService.skip(taskId, flowParams)`     | `skipType` 传 `PASS` / `REJECT`            |
| 查待办           | `taskService.list(queryTask)`              | 按`instanceId` 过滤 `flow_task`              |
| 查历史           | `hisTaskService.list(queryHisTask)`        | 按`instanceId` 过滤 `flow_his_task`          |

`FlowParams` 是贯穿发起和审批的参数载体（链式构建）：

```java
// 发起：指定跑哪份流程 + 发起人 + 流程变量
FlowParams.build().flowCode("leaveFlow").handler("张三").variable(variableMap);

// 审批：指定动作 + 办理人 + 权限 + 意见
FlowParams.build().skipType("PASS").handler("组长")
        .permissionFlag(List.of("role:leader")).message("同意");
```

---

## 六、业务表与引擎表如何协作（解耦）

`t_leave` 业务表通过 `instance_id` 关联引擎实例，并**冗余**了 `node_code` / `node_name` / `flow_status` 三个字段。这样：

- 业务侧查进度：直接 `select * from t_leave`，不用 join 引擎表；
- 引擎侧管流转：每次审批后，业务代码拿 `Instance.getNodeCode()/getNodeName()/getFlowStatus()` 回填业务表（见 `LeaveService.approve`）。

这就是生产上最常见的做法：**业务数据自己管，流程状态冗余一份副本**，两边用 `business_id` / `instance_id` 互相引用。

---

## 七、进阶实验

### 实验 1：再加一级审批（改流程定义）

生产上改流程 = 加一份**更高版本**的流程定义。本项目直接在 `leaveFlow.xml` 里加一个节点并调整 `skip`。因为 `/leave/init` 对已导入的定义会跳过，改完定义后需先把 `flow_definition` 中对应 `flowCode` 的记录删掉（`flow_node` / `flow_skip` 会被级联处理），再调 `/leave/init` 重新导入发布。真实场景通常会保留多版本定义（同 `flowCode` 多 `version`）。

### 实验 2：网关分支（条件审批）

把某个 `between` 节点换成 `nodeType="serial"` 互斥网关，给不同 `skip` 配 `skipCondition`（如请假天数>3 才进经理审批），配合 `FlowParams.variable` 里的变量做条件判断。这是工作流引擎最常用的能力之一。

### 实验 3：看引擎表数据

```sql
USE warm_flow_demo;
-- 流程定义/节点/跳转
SELECT * FROM flow_definition;
SELECT * FROM flow_node;
SELECT * FROM flow_skip;
-- 实例/待办/历史
SELECT * FROM flow_instance;
SELECT * FROM flow_task;
SELECT * FROM flow_his_task;
```

跑一次发起+审批后观察这几张表的数据变化，就能直观理解"定义→实例→任务→历史"的完整生命周期。

---

## 八、Warm-Flow 相比 Flowable / Activiti 的优势

| 对比项      | Activiti      | Flowable   | **Warm-Flow**                           |
| ----------- | ------------- | ---------- | --------------------------------------------- |
| 表数量      | 约 25 张      | 约 40+ 张  | **仅 7 张**                             |
| 学习成本    | 高（BPMN2.0） | 高         | **低，API 极简**                        |
| 集成        | 复杂          | 复杂       | **starter 即插即用**                    |
| 多 ORM 支持 | 仅 MyBatis    | 仅 MyBatis | **MyBatis/MyBatis-Plus/JPA/Easy-Query** |
| 适用场景    | 复杂企业流程  | 复杂高扩展 | **国产化、轻量级、快速审批**            |

> 参考：Flowable 官方曾列出 79 张表。Warm-Flow 用 7 张表覆盖了通过、退回、撤销、拿回、转办、委派、会签、票签、并行、互斥等主流审批能力。

---

## 九、离生产级还差哪几步

| 关键点                     | 说明                                                                                                                                          |
| -------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------- |
| **1. 权限接入**      | 本案例把`permissionFlag` 通过接口传参演示。生产上应实现 Warm-Flow 的**权限处理器**，从登录态/角色动态解析办理人权限，而不是接口硬编码 |
| **2. 流程设计器**    | Warm-Flow 提供 jar 包形式的设计器 UI（`warm-flow-plugin-ui-*`），生产上用可视化拖拽生成流程定义 JSON，而不是手写 XML                        |
| **3. 异步与事务**    | 本案例同步调用。真实审批常配合消息队列 + 分布式事务，保证"流程状态"与"业务状态"最终一致                                                       |
| **4. 多租户/软删除** | 7 张表都带`tenant_id` / `del_flag`，生产上配置租户处理器和逻辑删除即可多租户隔离                                                          |
| **5. 流程版本管理**  | 生产上流程定义要有版本号，改流程 = 新版本，老实例继续走老版本，避免影响在途业务                                                               |

---

## 十、核心原理（一图流）

```text
                流程定义（图纸）                流程实例（一次具体业务）
   db/leaveFlow.xml         flow_definition      flow_instance
   ┌──────────────┐  导入   ┌──────────────┐ 启动 ┌──────────────────┐
   │ start        │ ──────▶ │ flowCode=    │ ───▶ │ business_id=请假1 │
   │ leaderAudit  │         │ leaveFlow    │      │ nodeCode=leader  │
   │ managerAudit │         │ flow_node     │      │ flow_task（待办） │
   │ end          │         │ flow_skip     │      │ flow_his_task（历史）│
   └──────────────┘         └──────────────┘      └──────────────────┘
        importXml/publish         insService.start   taskService.skip
```

1. **定义阶段**：`importXml` 把 XML 解析成流程定义，`publish` 发布；
2. **实例化**：`insService.start(businessId, flowParams)` 按 `flowCode` 找已发布定义，生成实例 + 首个待办任务；
3. **流转**：`taskService.skip(taskId, flowParams)` 根据当前节点 + `skipType` 找跳转线，把待办转成历史、生成下一节点待办；
4. **结论**：一份流程定义可以被无限次实例化，每次实例都是一条独立的审批链——**定义一次、复用多次、互不干扰**。
