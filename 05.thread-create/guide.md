# Java 线程 4 种创建方式 — 面试讲解指南

---

## 先搞清楚：为什么要启动新线程？

### 🍳 生活例子理解

> **单线程 = 你一个人做饭**
> 
> 你要先洗菜 → 再切菜 → 再炒菜 → 再装盘。每一步都得等你做完上一步才能继续，中间哪怕在干等水烧开，你也什么都做不了，只能干站着。这就是**单线程**——所有事情排成一队，一件一件做。

> **多线程 = 你请了帮手**
> 
> 你洗菜的同时，让另一个人切菜，让第三个人炒菜。三个人同时干活，效率翻倍。水烧开需要等待的时候，你可以利用这个时间去切菜，不用干等。这就是**多线程**——多个任务同时进行。

### 💻 程序里的场景

以下情况你会**非常希望**启动新线程：

| 场景 | 单线程（不用新线程）会怎样 | 多线程（新线程）会怎样 |
|------|--------------------------|---------------------|
| 用户点击"下载文件"按钮 | 整个界面卡住不动，下载完才恢复，用户以为程序死了 | 后台悄悄下载，用户继续浏览其他页面 |
| 发送 1 万封邮件通知 | 界面卡死 10 分钟，用户直接关浏览器 | 后台慢慢发，用户秒收到"发送中"的提示 |
| Web 服务器处理 100 个请求 | 一次只能处理 1 个，第 100 个人要等前面 99 个都处理完 | 每个请求用一个线程处理，100 个人同时得到响应 |
| 需要同时查询 3 个不同的数据库 | 先查 A（等 2 秒）→ 再查 B（等 2 秒）→ 再查 C（等 2 秒）→ 总共 6 秒 | A、B、C 同时查，最快 2 秒全部拿到结果 |

### 📝 一句话总结

> **启动新线程 = 把耗时的、可以并行的任务丢到后台去做，让主线程（比如用户界面、HTTP 请求处理）不阻塞、不卡顿，同时利用多核 CPU 提升整体效率。**

### ⚠️ 副作用（也要知道）

多线程不是银弹，它会带来新问题：
- **线程安全**：多个线程同时修改同一个数据 → 数据错乱（需要用锁解决）
- **上下文切换开销**：CPU 在线程之间切换也有成本，线程不是越多越好
- **调试困难**：多线程的 Bug 往往随机出现，难以复现

所以实际项目中**推荐使用线程池**来精确控制线程数量，而不是无限制地开新线程。

---

## 一、四种方式总览

| 方式 | 类/接口 | 有无返回值 | 能否抛受检异常 | 典型使用 |
|------|----------|-----------|--------------|---------|
| 继承 Thread | `extends Thread` | 无 | 不能 | 极少用，仅作演示 |
| 实现 Runnable | `implements Runnable` | 无 | 不能 | 常见于简单任务 |
| 实现 Callable + FutureTask | `implements Callable<V>` | 有 | 能 | 需要返回结果的异步任务 |
| ExecutorService 线程池 | `ThreadPoolExecutor` | 可有可无 | 可有可无 | **生产环境首选** |

---

## 二、各方式详解

### 方式一：继承 Thread 类

```java
public class MyThread extends Thread {
    @Override
    public void run() {
        // 任务逻辑
    }
}
// 使用
new MyThread().start();
```

**优点**：简单直接，`start()` 即启动。
**缺点**：
- Java 单继承，继承了 Thread 就无法继承其他类，扩展性差
- 任务与线程耦合，违反单一职责原则

---

### 方式二：实现 Runnable 接口

```java
public class MyRunnable implements Runnable {
    @Override
    public void run() {
        // 任务逻辑
    }
}
// 使用
new Thread(new MyRunnable()).start();
```

**优点**：
- 解耦：任务逻辑独立于线程，可复用
- 任务类可继续继承其他类
- 同一实例可传给多个 Thread，适合资源共享

**缺点**：无返回值，不能抛受检异常

---

### 方式三：实现 Callable + FutureTask

```java
public class MyCallable implements Callable<String> {
    @Override
    public String call() throws Exception {
        return "result";
    }
}
// 使用
FutureTask<String> ft = new FutureTask<>(new MyCallable());
new Thread(ft).start();
String result = ft.get(); // 阻塞等待结果
```

**优点**：有返回值、可抛受检异常
**缺点**：`FutureTask.get()` 会阻塞调用线程

**注意**：可以配合 `get(long, TimeUnit)` 设置超时，避免永久阻塞

---

### 方式四：ExecutorService 线程池（ThreadPoolExecutor）

```java
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    2,                             // corePoolSize
    4,                             // maximumPoolSize
    60L, TimeUnit.SECONDS,         // keepAliveTime
    new LinkedBlockingQueue<>(10), // 有界工作队列
    new ThreadPoolExecutor.AbortPolicy() // 拒绝策略
);
executor.execute(() -> { /* 任务 */ });
```

**构造参数说明**：

| 参数 | 含义 | 说明 |
|------|------|------|
| corePoolSize | 核心线程数 | 即使空闲也保留 |
| maximumPoolSize | 最大线程数 | 线程数上限 |
| keepAliveTime | 空闲存活时间 | 超出 corePoolSize 的线程空闲多久后被回收 |
| workQueue | 工作队列 | **建议使用有界队列**，防止 OOM |
| handler | 拒绝策略 | AbortPolicy（默认抛异常） / CallerRunsPolicy / DiscardPolicy / DiscardOldestPolicy |

**实现原理**：
1. 提交任务 → 线程数 < corePoolSize → 创建新线程执行
2. 线程数 >= corePoolSize → 放入队列排队
3. 队列满 & 线程数 < maximumPoolSize → 创建新线程（临时线程）
4. 队列满 & 线程数 >= maximumPoolSize → 执行拒绝策略

---

## 三、⭐ 推荐使用方式 & 原因

### 推荐：方式四 —— ExecutorService 线程池（ThreadPoolExecutor）

**核心原因**：

1. **资源可控**：通过核心线程数、最大线程数、队列容量限制并发，防止无限制创建线程导致 OOM
2. **复用线程**：避免频繁创建/销毁线程的开销，降低 GC 压力
3. **解耦提交与执行**：任务只需提交，执行策略由线程池管理
4. **功能丰富**：支持定时、周期任务（ScheduledThreadPoolExecutor）、Future 异步获取结果
5. **优雅关闭**：调用 `shutdown()` 可平滑关闭，等待已提交任务执行完毕

### 为什么不推荐方式一~三？

| 方式 | 不推荐原因 |
|------|-----------|
| 继承 Thread | 单继承限制，任务与线程强耦合 |
| 实现 Runnable | 无返回值，不适合需要结果的场景 |
| Callable + FutureTask | 每次任务都要 new Thread()，资源不可控；`get()` 阻塞 |

### 生产最佳实践

```java
// 阿里巴巴 Java 开发手册推荐：手动创建线程池，禁止使用 Executors
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    corePoolSize,
    maximumPoolSize,
    keepAliveTime,
    TimeUnit.SECONDS,
    new LinkedBlockingQueue<>(queueCapacity),  // 有界队列！
    new ThreadPoolExecutor.CallerRunsPolicy()   // 推荐：让调用线程自己执行
);
```

---

## 四、演示 API 接口

项目启动后访问以下接口，观察控制台输出：

| 接口 | 方式 | 请求示例 |
|------|------|---------|
| `GET /api/thread/thread` | 继承 Thread | http://localhost:10000/api/thread/thread |
| `GET /api/thread/runnable` | 实现 Runnable | http://localhost:10000/api/thread/runnable |
| `GET /api/thread/callable` | Callable + FutureTask | http://localhost:10000/api/thread/callable |
| `GET /api/thread/pool` | ThreadPoolExecutor | http://localhost:10000/api/thread/pool |

控制台输出示例：
```
[Thread-3] 23:41:50.123 —— 方式一：继承 Thread 类
[Thread-4] 23:41:50.124 —— 方式二：实现 Runnable 接口
[Thread-5] 23:41:50.125 —— 方式三：实现 Callable + FutureTask
[pool-1-thread-1] 23:41:50.126 —— 方式四：ExecutorService 线程池 (ThreadPoolExecutor)