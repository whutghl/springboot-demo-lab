# RedisBloom 布隆过滤器 — 教学指南

## 概述

本项目是一个 **Spring Boot 2.7 + RedisBloom（Redis Stack）** 的教学级布隆过滤器集成示例。

**核心目标**：理解布隆过滤器如何解决**缓存穿透**问题。

---

## 一、快速启动

### 1.1 环境要求

| 组件 | 版本 |
|------|------|
| Java | 17 |
| Maven | 3.x |
| Redis | 需安装 Redis Stack（含 RedisBloom 模块） |
| 操作系统 | Linux |

### 1.2 启动项目

```bash
cd /data/app/ai/ms/bulong/bulong-redis2

# 编译并启动
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 mvn spring-boot:run
```

启动后观察控制台日志，确认布隆过滤器初始化成功：
```
=== 开始初始化布隆过滤器 ===
[初始化] BF.RESERVE 成功: key=bloom:user:ids, error_rate=0.01, capacity=10000
[初始化] 已预填 5 条演示数据: [1001, 1002, 1003, 1004, 1005]
=== 布隆过滤器初始化完成 ===
```

---

## 二、代码结构

```
src/main/java/com/example/spdemo/
├── SpdemoApplication.java              ← 启动类，含 ApplicationRunner 自动初始化
├── controller/
│   └── HelloController.java            ← 原有接口
└── redisbloom/
    ├── RedisBloomConfig.java           ← 配置常量：key名、容量、误判率
    ├── RedisBloomService.java          ← 核心服务：BF.RESERVE / BF.ADD / BF.EXISTS
    └── RedisBloomController.java       ← REST 接口：/redis-bloom/add 和 /redis-bloom/contains
```

### 各文件职责

| 文件 | 职责 |
|------|------|
| `RedisBloomConfig.java` | 定义布隆过滤器的 key、预期容量(10000)、误判率(0.01)。**所有配置集中在一处。** |
| `RedisBloomService.java` | 使用 `RedisTemplate.execute()` 执行原生 RedisBloom 命令。**零第三方依赖。** |
| `RedisBloomController.java` | 提供 HTTP 接口用于手动添加和查询，方便调试和理解。 |
| `SpdemoApplication.java` | 通过 `ApplicationRunner` 在启动时自动调用 `initBloomFilter()`。 |

---

## 三、核心概念

### 3.1 布隆过滤器是什么？

一个**概率性数据结构**，用于回答：

> "这个元素**可能**在集合里吗？"

它只能给出两种答案：

| 答案 | 含义 | 准确性 |
|------|------|--------|
| **可能存在** | 元素也许在集合里 | 有一定误判概率（False Positive） |
| **一定不存在** | 元素绝对不在集合里 | 100% 准确 |

**关键特性**：布隆过滤器**不会漏判**（False Negative 概率为 0），但**会误判**（False Positive 概率 > 0）。

### 3.2 缓存穿透问题

```
正常请求流程：
  用户 → 缓存(Redis) → 命中 → 返回
  用户 → 缓存(Redis) → 未命中 → 数据库 → 回写缓存 → 返回

缓存穿透（攻击）：
  攻击者用大量不存在的 ID 请求
  → 缓存永远不命中（因为 ID 不存在）
  → 每次请求都击穿到数据库
  → 数据库压力剧增，可能崩溃
```

### 3.3 布隆过滤器解决方案

```
                    ┌──────────────┐
  用户请求 ──────→  │ 布隆过滤器   │
   (ID=xxx)        │              │
                    │ BF.EXISTS    │
                    │ bloom:user:ids│
                    │      xxx     │
                    └──────┬───────┘
                           │
              ┌────────────┴────────────┐
              │                         │
        返回 0                    返回 1
      "一定不存在"              "可能存在"
              │                         │
              ▼                         ▼
      直接返回 404              查询 Redis 缓存
      （不查 DB！）                  │
      拦截缓存穿透          ┌────────┴────────┐
                           │                  │
                       命中返回            未命中
                           │                  │
                           ▼                  ▼
                        返回数据         查询数据库
                                            │
                                            ▼
                                         回写缓存
                                         返回数据
```

**核心价值**：被布隆过滤器判定为"一定不存在"的请求，在缓存之前就被拦截，永远不会到达数据库。

---

## 四、RedisBloom 命令详解

### 4.1 BF.RESERVE — 创建布隆过滤器

```
BF.RESERVE bloom:user:ids 0.01 10000
          │              │    │
          │              │    └── capacity：预计存储 10000 个元素
          │              └── error_rate：误判率 1%
          └── key：过滤器名称
```

底层会自动计算：
- **位数组大小 m** ≈ `-(n × ln(p)) / (ln(2))²` = `-(10000 × ln(0.01)) / 0.48` ≈ 95850 bits ≈ 12KB
- **哈希函数数量 k** ≈ `(m/n) × ln(2)` ≈ 7

### 4.2 BF.ADD — 添加元素

```
BF.ADD bloom:user:ids "1001"
```

工作流程：
1. 将 "1001" 经过 k 个哈希函数得到 k 个哈希值
2. 每个哈希值对位数组长度取模，得到 k 个位置
3. 将这 k 个位置（bit）都置为 1

返回值：
- `1`：成功添加（至少有一个位从 0→1）
- `0`：所有位已经是 1（元素可能已存在）

### 4.3 BF.EXISTS — 查询元素

```
BF.EXISTS bloom:user:ids "1001"
```

工作流程：
1. 计算元素的 k 个哈希位置
2. 检查这 k 个位置是否**全部**为 1

返回值：
- `1`：所有位都是 1 → **可能存在**
- `0`：至少有一个位是 0 → **一定不存在**

---

## 五、验证操作

### 5.1 redis-cli 验证 BF.RESERVE

```bash
redis-cli -p 16379 -a ghl

# 1. 查看布隆过滤器信息
127.0.0.1:16379> BF.INFO bloom:user:ids
 1) Capacity              # 配置的容量
 2) Size                  # 位数组实际大小
 3) Number of filters     # 子过滤器数量（Scalable Bloom Filter 特性）
 4) Number of items inserted  # 已添加元素数
 5) Expansion rate        # 扩容比例

# 2. 验证预填的演示数据
127.0.0.1:16379> BF.EXISTS bloom:user:ids 1001
(integer) 1              # ← 可能存在（我们确实添加过）

127.0.0.1:16379> BF.EXISTS bloom:user:ids 1005
(integer) 1

# 3. 验证"一定不存在"
127.0.0.1:16379> BF.EXISTS bloom:user:ids 9999
(integer) 0              # ← 一定不存在（从未添加过）

# 4. 查看 key 是否存在
127.0.0.1:16379> EXISTS bloom:user:ids
(integer) 1              # ← key 存在

# 5. 查看 key 类型（RedisBloom 注册了自定义类型）
127.0.0.1:16379> TYPE bloom:user:ids
MBbloom--                # ← 自定义类型，不是普通的 string/hash
```

### 5.2 curl 验证接口

```bash
# ======== 添加元素 ========

# 添加一个新元素
curl "http://localhost:10000/redis-bloom/add?value=8888"
# → {"action":"add","value":"8888","isNew":true,"bloomFilter":"bloom:user:ids",
#    "note":"元素已成功添加到布隆过滤器"}

# 重复添加同一个元素
curl "http://localhost:10000/redis-bloom/add?value=8888"
# → {"action":"add","value":"8888","isNew":false,...
#    "note":"元素可能已存在（所有哈希位已是1）"}

# ======== 查询元素 ========

# 查询预填的演示数据（一定存在）
curl "http://localhost:10000/redis-bloom/contains?value=1001"
# → {"mayExist":true,"verdict":"可能存在",
#    "suggestion":"继续查询缓存/数据库以确认"}

# 查询从未添加的数据（一定不存在）
curl "http://localhost:10000/redis-bloom/contains?value=9999"
# → {"mayExist":false,"verdict":"一定不存在",
#    "suggestion":"直接返回空结果，无需查询数据库（缓存穿透拦截成功!）"}
```

---

## 六、观察误判（False Positive）实验

这是本教程**最重要的教学实验**，让你直观感受布隆过滤器的误判现象。

### 6.1 实验原理

布隆过滤器的位数组大小是固定的。当越来越多元素被添加后，位数组中越来越多的 bit 被置 1（变得"密集"），一个从未添加过的随机元素的所有哈希位置恰好都是 1 的概率就会上升——这就是 **False Positive**。

### 6.2 实验步骤

```bash
# Step 1: 先查询一个肯定不存在的大数（此时位数组还很稀疏）
curl "http://localhost:10000/redis-bloom/contains?value=XYZ999"
# → mayExist=false（位数组稀疏，误判概率低）

# Step 2: 大量灌入数据，让位数组变密集
# 添加 5000 条数据
for i in $(seq 2000 7000); do
  curl -s "http://localhost:10000/redis-bloom/add?value=${i}" > /dev/null
done
echo "已添加 5000 条数据"

# Step 3: 再次查询同一个不存在的值
curl "http://localhost:10000/redis-bloom/contains?value=XYZ999"
# → 大概率还是 mayExist=false（5000 < 10000 capacity，还不算密集）

# Step 4: 继续填到 capacity（10000 条）
for i in $(seq 7001 10000); do
  curl -s "http://localhost:10000/redis-bloom/add?value=${i}" > /dev/null
done
echo "已填满到 capacity=10000"

# Step 5: 用大量随机值测试，观察误判
for i in $(seq 20000 20100); do
  echo -n "value=$i → "
  curl -s "http://localhost:10000/redis-bloom/contains?value=${i}" | grep -o '"mayExist":[^,]*'
done
# 观察输出：在 100 个随机值中，约 1% 会出现 mayExist=true
# 这就是 0.01 FPP 的直观体现！
```

### 6.3 为什么 capacity 不能设太小？

```bash
# 实验：只设 capacity=100 但实际添加 10000 个元素
# （本项目的 capacity=10000，无法演示此场景，以下是原理说明）
```

当实际元素数远超 capacity 时：
- 位数组中几乎所有 bit 都是 1
- 误判率可能飙升至 50%+，布隆过滤器几乎失效
- **教训**：capacity 必须 ≥ 你的实际数据规模

---

## 七、技术实现细节

### 7.1 为什么用 `RedisTemplate.execute(RedisCallback)` 而不是封装库？

```
优点：
  ✅ 零额外依赖，只用 Spring Data Redis
  ✅ 完全透明，你能看到每一行代码对应什么 Redis 命令
  ✅ 不需要学习新的 API（如 JReBloom）
  ✅ 对 RedisBloom 新命令的兼容性最好

缺点：
  ❌ 代码稍显底层
  ❌ 没有类型安全的 API
```

对于教学目的，透明性 > 便利性。

### 7.2 为什么不手写 SETBIT/GETBIT？

```
手写方案的问题：
  ❌ 需要自己实现多个哈希函数（如 MurmurHash3 的多个 seed）
  ❌ 需要管理位数组的分配和扩容
  ❌ 性能和 RedisBloom C 实现差距巨大
  ❌ 无法享受 RedisBloom 的 Scalable Bloom Filter 特性

RedisBloom 的优势：
  ✅ C 语言实现，性能极高
  ✅ 内置 Scalable Bloom Filter（自动扩容）
  ✅ 命令简洁：BF.ADD / BF.EXISTS
  ✅ 还支持 BF.INSERT（批量添加）、BF.MADD/BF.MEXISTS 等高级命令
```

### 7.3 为什么不推荐 Guava BloomFilter？

```
Guava BloomFilter：
  ❌ 纯内存，进程重启后丢失
  ❌ 分布式环境下，每个实例都有一份独立副本，状态不一致
  ❌ 无法在多个微服务间共享

RedisBloom：
  ✅ 持久化到 Redis（支持 RDB/AOF）
  ✅ 所有服务实例共享同一份布隆过滤器
  ✅ 天然支持分布式部署
```

---

## 八、生产环境注意事项

### 8.1 容量规划

```
capacity 应该 ≥ 你的最大数据量 × 1.2（留 20% buffer）

例如：
  预计用户数：100 万
  → capacity 至少设为 120 万
```

### 8.2 误判率选择

| 场景 | 推荐 FPP |
|------|----------|
| 缓存穿透拦截（能容忍少量漏网） | 0.01 (1%) |
| 推荐系统去重（要比较精准） | 0.001 (0.1%) |
| 垃圾邮件过滤 | 0.0001 (0.01%) |

### 8.3 数据预热策略

```java
// 生产环境建议：
// 1. 从数据库分页读取所有已有ID
// 2. 使用 BF.INSERT（批量添加，比逐条 BF.ADD 快很多）
// 3. 在预热期间，服务不对外暴露

// 增量更新：
// 新增数据时，同时调 add() 写入布隆过滤器
// 删除数据时，布隆过滤器无法删除（需使用布谷鸟过滤器等替代方案）
```

### 8.4 删除问题

布隆过滤器**不支持删除**。如果业务需要删除操作，考虑：
- 布谷鸟过滤器（Cuckoo Filter，RedisBloom 也支持 `CF.*` 命令）
- 计数布隆过滤器（Counting Bloom Filter）
- 定时重建布隆过滤器（例如每天凌晨从数据库全量重建）

---

## 九、常见问题

### Q1: 误判会导致什么问题？

在缓存穿透场景中，误判只是让一个不存在的 ID "漏网"到了数据库层。由于误判率只有 1%，100 次攻击中只有 1 次会打到数据库——这已经大幅降低了数据库压力。

### Q2: 布隆过滤器会漏判吗？

**不会。** 如果 BF.EXISTS 返回 0，说明该元素绝对不在过滤器中。这是布隆过滤器的数学保证。

### Q3: 内存占用有多大？

本示例（capacity=10000, FPP=0.01）：约 12KB。

扩展到更大规模：
- 100 万元素，1% FPP：约 1.2MB
- 1 亿元素，0.1% FPP：约 180MB

### Q4: RedisBloom 和 Redisson 的 RBloomFilter 有什么区别？

| | RedisBloom | Redisson RBloomFilter |
|---|---|---|
| 实现方式 | Redis C 模块 | 纯 Java + SETBIT/GETBIT |
| 性能 | 极高 | 一般（多次网络往返） |
| 扩容 | Scalable Bloom Filter | 不支持自动扩容 |
| 依赖 | 需要 Redis Stack | 只需标准 Redis |

---

## 十、扩展阅读

- [RedisBloom 官方文档](https://redis.io/docs/data-types/probabilistic/bloom-filter/)
- [布隆过滤器原理（Wikipedia）](https://en.wikipedia.org/wiki/Bloom_filter)
- [缓存穿透的三种解决方案对比](https://redis.io/docs/manual/patterns/)
