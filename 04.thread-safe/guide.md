# Java 线程安全性教学 Demo —— 完整指南

> **目标读者**：正在学习 Spring Boot 的 Java 中级开发者
> **Spring Boot 版本**：2.7.x
> **Java 版本**：1.8+
> **验证方式**：并发 HTTP 请求（ab / JMeter / wrk / 手写多线程测试）

---

## 一、项目结构速览

```
com.example.spdemo
├── entity
│   └── User.java                     # 演示用 POJO
├── controller
│   └── ThreadSafeController.java     # 统一 REST 入口
└── service
    ├── PrimitiveTypeService.java     # 基本类型 (int / long / Atomic)
    ├── ObjectReferenceService.java   # 对象引用与 POJO
    ├── CollectionService.java        # 集合类 vs 线程安全替代
    └── BeanScopeService.java         # Spring Singleton Bean 作用域
```

---

## 二、各场景详解

### 1. 基本类型 — `int` / `long`

| 写法                   | 线程安全？ | 原因                                       |
| ---------------------- | ---------- | ------------------------------------------ |
| 成员变量 `int count` | ❌ 不安全  | `i++` 拆分为「读-改-写」三步，非原子操作 |
| 局部变量 `int count` | ✅ 安全    | 栈私有，每个线程独立                       |
| `AtomicInteger`      | ✅ 安全    | CAS 原子指令保证无锁并发                   |

**并发风险点**：

- 两个线程同时读取 `count = 5`
- 各自加 1 后都写回 `6`
- 期望 `7`，实际 `6` → **丢失更新（Lost Update）**

**正确写法**：

```java
private final AtomicInteger safeCounter = new AtomicInteger(0);
safeCounter.incrementAndGet(); // CAS 自旋保证原子性
```

**面试追问**：

> AtomicInteger 的 CAS 有什么缺点？
> — **ABA 问题**：值从 A→B→A，CAS 无法感知中间变化。可用 `AtomicStampedReference` 带版本号解决。

---

### 2. 对象引用 — 普通 POJO

| 写法                   | 线程安全？ | 原因                                     |
| ---------------------- | ---------- | ---------------------------------------- |
| 共享可变 POJO 成员变量 | ❌ 不安全  | 多线程同时修改对象内部字段，结果混乱     |
| 方法内新建局部对象     | ✅ 安全    | 对象在堆上新建，但引用仅存在于当前线程栈 |

**并发风险点**：

- 线程 A 调用 `setName("A")` 后 sleep
- 线程 B 调用 `setName("B")`
- 线程 A 醒来读取 → 拿到 `"B"`，出现数据交叉污染

**正确写法**：

```java
public Result process(String name) {
    User localUser = new User(name); // 每次新建，不共享
    // ... 处理
    return result;
}
```

**面试追问**：

> `final` 修饰对象引用能保证线程安全吗？
> — **不能**。`final` 只保证引用不可变，对象内部字段仍可修改。需要对象本身不可变（如 String、Integer、自定义不可变类）。

---

### 3. 集合类 — `ArrayList` / `HashMap` / `HashSet`

| 集合          | 线程安全？ | 替代方案                                                            |
| ------------- | ---------- | ------------------------------------------------------------------- |
| `ArrayList` | ❌ 不安全  | `CopyOnWriteArrayList` / `Collections.synchronizedList`         |
| `HashMap`   | ❌ 不安全  | `ConcurrentHashMap`                                               |
| `HashSet`   | ❌ 不安全  | `ConcurrentHashMap.newKeySet()` / `Collections.synchronizedSet` |

**ArrayList 并发风险**：

- `size++` 非原子 → 元素覆盖丢失
- 扩容时 `grow()` 可能被多个线程同时触发 → `ArrayIndexOutOfBoundsException`

**HashMap 并发风险**：

- JDK 7：resize 时链表头插法可能导致**死循环（Infinite Loop）**
- JDK 8：改为尾插法，解决了死循环，但仍有**数据丢失**问题
- 多线程同时 put 到同一桶位，可能覆盖彼此节点

**正确写法**：

```java
// 读多写少推荐
private final List<String> safeList = new CopyOnWriteArrayList<>();

// 通用高并发推荐
private final Map<String, Integer> safeMap = new ConcurrentHashMap<>();

// Java 8+ 线程安全 Set
private final Set<String> safeSet = ConcurrentHashMap.newKeySet();
```

```

```

**面试追问**：

> `Collections.synchronizedMap` 与 `ConcurrentHashMap` 的区别？
> — `synchronizedMap` 对整个 Map 加锁（粗粒度），并发度低；`ConcurrentHashMap` 分段/节点级锁（细粒度），并发度高。

---

### 4. 线程安全替代方案 — `Atomic` / `ConcurrentHashMap` / `CopyOnWriteArrayList`

| 类                                 | 适用场景       | 注意点                                                  |
| ---------------------------------- | -------------- | ------------------------------------------------------- |
| `AtomicInteger` / `AtomicLong` | 计数器、序列号 | 适合简单原子操作；复杂逻辑仍需 `synchronized` 或锁    |
| `ConcurrentHashMap`              | 高并发读写 Map | `size()` / `containsValue()` 可能不精确（弱一致性） |
| `CopyOnWriteArrayList`           | 读极多、写极少 | 每次写复制整个数组，内存开销大，不适合写频繁场景        |

**面试追问**：

> `CopyOnWriteArrayList` 的迭代器有什么特点？
> — 迭代器基于**快照（snapshot）**，遍历期间不受后续修改影响，不会抛 `ConcurrentModificationException`。

---

### 5. Spring Bean 作用域 — Singleton 的陷阱

| 设计方式                        | 线程安全？ | 原因                                         |
| ------------------------------- | ---------- | -------------------------------------------- |
| Singleton Bean + 可变成员变量   | ❌ 不安全  | 整个应用只有一个 Bean 实例，所有请求线程共享 |
| Singleton Bean +`AtomicXxx`   | ✅ 安全    | 共享状态但通过原子类/锁保证操作安全          |
| Singleton Bean + 无状态（推荐） | ✅ 安全    | 所有数据走参数和返回值，中间结果用局部变量   |

**Spring 官方最佳实践**：

> **Service / DAO 层应保持无状态（stateless）**，不要定义可变成员变量。

**正确写法**：

```java
@Service
public class OrderService {
    // ❌ 不要这样
    // private int counter = 0;

    // ✅ 无状态：所有数据通过方法参数传递
    public Order createOrder(CreateOrderRequest request) {
        Order order = new Order();
        // ... 处理
        return order;
    }
}
```

**面试追问**：

> Spring 的 `@Scope("prototype")` 能完全解决线程安全问题吗？
> — 能解决 Bean 内部状态共享问题，但**不推荐使用**。prototype Bean 创建和销毁成本高，且循环依赖时容易出问题。无状态设计才是根本解法。

---

## 三、如何验证（压测步骤）

### 环境准备

确保项目已启动：

```bash
mvn spring-boot:run
# 或先打包再运行
mvn clean package -DskipTests
java -jar target/spdemo-0.0.1-SNAPSHOT.jar
```

### 使用 Apache Bench (ab) 验证

> 💡 端口默认为 `10000`，如果配置了其他端口请替换。
> GET 请求可直接用 ab 压测；POST 请求需先手动调用一次 curl 重置。

---

**1. 基本类型 — int 成员变量（不安全）**

```bash
ab -n 1000 -c 50 -v 2 "http://localhost:10000/api/primitive/unsafe?times=1000"
```

- 期望 `expected = 1000`，实际 `actual` 往往 **< 1000**（丢失更新）

---

**2. 基本类型 — AtomicInteger 单步操作（看似安全实则有坑）**

```bash
ab -n 1000 -c 50 -v 2 "http://localhost:10000/api/primitive/safe?times=1000"
```

- `set(0)` 被多个线程互相覆盖，`match` 往往为 `false`

---

**3. 基本类型 — AtomicInteger.addAndGet（也并非安全）**

```bash
ab -n 1000 -c 50 -v 2 "http://localhost:10000/api/primitive/atomic-add?times=1000"
```

- `set(0)` 与 `addAndGet(times)` 之间被其他线程插入，`match` 为 `false`

---

**4. 基本类型 — synchronized 加锁（真正安全）**

```bash
ab -n 1000 -c 50 -v 2 "http://localhost:10000/api/primitive/synced?times=1000"
```

- 无论并发多高，`match` 始终为 `true`

---

**5. 基本类型 — 局部变量（天然安全）**

```bash
ab -n 1000 -c 50 -v 2 "http://localhost:10000/api/primitive/local?times=1000"
```

- 局部变量栈私有，`match` 始终为 `true`

---

**6. 对象引用 — 共享可变 POJO（不安全）**

直接运行 JUnit 测试类，自动完成并发请求、数据比对、输出结论：

```bash
mvn test -Dtest=ObjectReferenceThreadSafetyTest -pl . -DskipTests=false
```

> 💡 测试原理：启动 Spring Boot（随机端口），用 20 个线程并行发送 100 个携带随机 name/age 的请求到 `/api/object/unsafe`，然后逐条解析响应并比对 `currentName` 和 `currentAge` 是否匹配预期值。如果出现交叉数据（如 name=Alice, age=30 但 Alice 实际 age 应为 25），则输出污染日志并统计总数。

- 无需手动拼接 URL，无需安装 jq，无需人工翻看终端
- 测试控制台会输出：总请求数、数据污染数、数据正常数、最终共享状态

---

**7. 对象引用 — 局部新建对象（安全）**

```bash
ab -n 500 -c 50 "http://localhost:10000/api/object/safe?name=Alice&age=25"
```

- 每次请求新建对象，方法内处理，不会互相干扰

---

**8. 集合类 — ArrayList（不安全）**

```bash
ab -n 500 -c 50 -v 2 "http://localhost:10000/api/collection/list/unsafe?count=500"
```

- `size` 可能 **< 500**（元素覆盖丢失），极端情况抛 `ArrayIndexOutOfBoundsException`

---

**9. 集合类 — CopyOnWriteArrayList（安全）**

```bash
ab -n 500 -c 50 -v 2 "http://localhost:10000/api/collection/list/safe?count=500"
```

- `match` 始终为 `true`

---

**10. 集合类 — HashMap（不安全）**

```bash
ab -n 500 -c 50 "http://localhost:10000/api/collection/map/unsafe?count=500"
```

- `match` 大概率返回 `false`，极端情况下可能抛出异常

---

**11. 集合类 — ConcurrentHashMap（安全）**

```bash
ab -n 500 -c 50 "http://localhost:10000/api/collection/map/safe?count=500"
```

- `match` 始终为 `true`

---

**12. 集合类 — HashSet（不安全）**

```bash
ab -n 500 -c 50 "http://localhost:10000/api/collection/set/unsafe?count=500"
```

- 数据可能丢失或出现异常

---

**13. 集合类 — ConcurrentHashMap.newKeySet（安全）**

```bash
ab -n 500 -c 50 "http://localhost:10000/api/collection/set/safe?count=500"
```

- `match` 始终为 `true`

---

**14. Bean 作用域 — 成员变量计数（不安全）**

```bash
# 先重置
curl -X POST "http://localhost:10000/api/bean/reset"

# 再压测
ab -n 1000 -c 50 -v 2 "http://localhost:10000/api/bean/unsafe?client=test"
```

- 多次压测后 `currentCount` 大概率 **< 1000**

---

**15. Bean 作用域 — AtomicInteger 计数（安全）**

```bash
# 先重置
curl -X POST "http://localhost:10000/api/bean/reset"

# 再压测
ab -n 1000 -c 50 "http://localhost:10000/api/bean/safe?client=test"
```

- `match` 始终为 `true`

---

**16. Bean 作用域 — 无状态设计（推荐，天然安全）**

```bash
ab -n 1000 -c 50 "http://localhost:10000/api/bean/stateless?client=test&input=10"
```

- 所有数据通过参数传递，无共享状态，天然安全

### 使用 wrk（更高并发）

```bash
wrk -t12 -c400 -d30s "http://localhost:10000/api/primitive/unsafe?times=1000"
```

---

## 四、总结速查表

| 分类        | 类型/写法                   | 线程安全 | 推荐替代/写法                      |
| ----------- | --------------------------- | -------- | ---------------------------------- |
| 基本类型    | `int` / `long` 成员变量 | ❌       | `AtomicInteger` / `AtomicLong` |
| 基本类型    | `int` / `long` 局部变量 | ✅       | 无需替代                           |
| 对象引用    | 共享可变 POJO               | ❌       | 方法内新建局部对象                 |
| 集合        | `ArrayList`               | ❌       | `CopyOnWriteArrayList`           |
| 集合        | `HashMap`                 | ❌       | `ConcurrentHashMap`              |
| 集合        | `HashSet`                 | ❌       | `ConcurrentHashMap.newKeySet()`  |
| Spring Bean | Singleton + 可变状态        | ❌       | 无状态设计（参数+局部变量）        |
| Spring Bean | Singleton + Atomic          | ✅       | 计数器等简单场景可用               |

---

## 五、常见面试题（进阶）

1. **volatile 能保证线程安全吗？**— 只能保证**可见性**和**有序性**，不保证**原子性**。适合作为状态标志位（`boolean running = true`），不适合计数器。
2. **`synchronized` 与 `ReentrantLock` 的区别？**— `synchronized` 是 JVM 关键字，自动释放锁；`ReentrantLock` 是 API 级锁，支持公平锁、中断、条件变量、尝试获取等高级功能。
3. **`ThreadLocal` 是解决线程安全问题的吗？**— 不是传统意义的「共享安全」，而是**彻底隔离**：每个线程一份独立副本。适合存放用户上下文、数据库连接等。注意内存泄漏风险（用完需 `remove()`）。
4. **为什么 String 是线程安全的？**— String 是**不可变对象（Immutable）**：值创建后不可修改，任何修改都返回新对象。不可变对象天然线程安全。
5. **单例模式如何保证线程安全？**
   — 枚举（最佳）、静态内部类（懒加载）、双重检查锁定（DCL + volatile）。避免单纯 `synchronized` 方法导致的性能问题。

---

## 六、扩展阅读

- 《Java 并发编程实战》（Java Concurrency in Practice）— Brian Goetz
- 《深入理解 Java 虚拟机》— 周志明（JMM 内存模型章节）
- Spring 官方文档：[Bean Scopes](https://docs.spring.io/spring-framework/docs/5.3.x/reference/html/core.html#beans-factory-scopes)
