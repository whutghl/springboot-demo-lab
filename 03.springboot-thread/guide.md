# Spring Boot 2.7 线程池集成指南

> 基于 `spdemo` 项目（Spring Boot 2.7.18 + Java 8），从零教你把 `@Async` + 线程池跑起来。

---

## 一、分析现状——Demo 里缺了什么

你的 Demo 初始状态：


| 文件                | 内容                                                                    |
| ------------------- | ----------------------------------------------------------------------- |
| `SpdemoApplication` | 裸的`@SpringBootApplication`，无异步相关注解                            |
| `HelloController`   | 一个简单的`GET /api/hello`，逻辑直接写在 Controller 里，没有 Service 层 |
| `pom.xml`           | `spring-boot-starter-web` + `spring-boot-starter-test`                  |

要加线程池，缺的是：


| 缺失项                      | 说明                                                                                                                                                |
| --------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------- |
| **启动注解 `@EnableAsync`** | 不加它，`@Async` 完全不生效                                                                                                                         |
| **线程池配置**              | 没有`ThreadPoolTaskExecutor` Bean，Spring 会用默认的 `SimpleAsyncTaskExecutor`——每次来任务就新建线程，用完就扔，**高并发下直接 OOM**              |
| **Service 层**              | Controller 里直接写了业务逻辑。`@Async` 必须放在能被 Spring AOP 代理的 Bean 方法上才能生效                                                          |
| **依赖**                    | 好消息：**不需要额外加依赖**。`@Async`、`@EnableAsync`、`ThreadPoolTaskExecutor` 全在 `spring-context` 中，`spring-boot-starter-web` 已经传递引入了 |

---

## 二、最小化改造

### 2.1 启动类加 `@EnableAsync`

```java
package com.example.spdemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync  // 必须加！这是开启异步的总开关
public class SpdemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpdemoApplication.class, args);
    }
}
```

### 2.2 新增线程池配置类

```java
package com.example.spdemo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class ExecutorConfig {

    @Bean("asyncExecutor")
    public Executor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心线程数：一直存活的线程数。默认值是 1。
        // 轻量任务 1-2 就够；高并发可以调到 CPU 核心数。
        executor.setCorePoolSize(2);

        // 最大线程数：忙的时候最多撑到多少个线程。默认值是 Integer.MAX_VALUE（危险！）。
        // 一定要设一个上限，建议为核心数的 2~4 倍。
        executor.setMaxPoolSize(4);

        // 队列容量：核心线程忙不过来时，新任务先进队列排队。
        // 默认是 Integer.MAX_VALUE（无界队列 = 内存炸弹），必须设一个合理值。
        executor.setQueueCapacity(50);

        // 线程名前缀：在日志/控制台一眼认出是哪个池的线程。
        executor.setThreadNamePrefix("async-demo-");

        // 拒绝策略：队列满、线程也满时怎么处理新任务？
        // AbortPolicy（默认）：直接抛异常，新手很容易踩这个坑。
        // CallerRunsPolicy：让提交任务的那个线程自己执行，至少不丢任务。
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 非核心线程空闲超时时间（秒），超过后回收。
        executor.setKeepAliveSeconds(60);

        // 核心线程是否允许超时回收（默认 false，核心线程永不回收）。
        executor.setAllowCoreThreadTimeOut(false);

        executor.initialize();
        return executor;
    }
}
```

**参数速查表：**


| 参数                       | 默认值                  | 建议                                  |
| -------------------------- | ----------------------- | ------------------------------------- |
| `corePoolSize`             | 1                       | 改为 CPU 核心数或业务需要的常驻数     |
| `maxPoolSize`              | `Integer.MAX_VALUE`     | **一定要限制**，建议为核心数的 2~4 倍 |
| `queueCapacity`            | `Integer.MAX_VALUE`     | 无界队列=内存炸弹，设一个合理值       |
| `rejectedExecutionHandler` | `AbortPolicy`（抛异常） | 换成`CallerRunsPolicy`，至少不丢任务  |

### 2.3 新建 Service，方法加 `@Async`

```java
package com.example.spdemo.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class AsyncService {

    /**
     * @Async("asyncExecutor") 指定用我们自定义的线程池。
     * 返回值用 CompletableFuture 包一下，调用方可以等结果，也可以不等。
     */
    @Async("asyncExecutor")
    public CompletableFuture<String> doAsyncWork() {
        String threadName = Thread.currentThread().getName();
        System.out.println("[异步任务] 执行线程: " + threadName);

        // 模拟耗时操作（调第三方接口、写文件等）
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("[异步任务] 完成! 线程: " + threadName);
        return CompletableFuture.completedFuture("异步任务完成，线程: " + threadName);
    }

    /**
     * 不加 @Async 做对比——能看到它在哪个线程上跑。
     */
    public String doSyncWork() {
        String threadName = Thread.currentThread().getName();
        System.out.println("[同步任务] 执行线程: " + threadName);
        return "同步任务完成，线程: " + threadName;
    }
}
```

**为什么要这样写？**

- `@Async` 让方法跑在你的 `asyncExecutor` 线程池里，而不是 Tomcat 的请求处理线程
- 返回 `CompletableFuture` 而不是 `void`，这样调用方可以 `join()` 等结果、`get()` 拿异常
- 线程名前缀 `async-demo-` 让你在控制台能一眼辨认

### 2.4 改造 Controller，注入 Service

```java
package com.example.spdemo.controller;

import com.example.spdemo.service.AsyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api")
public class HelloController {

    @Autowired
    private AsyncService asyncService;

    @GetMapping("/hello")
    public Map<String, Object> hello() {
        // 打印调用方线程名（应该是 http-nio-xxx，即 Tomcat 线程）
        String callerThread = Thread.currentThread().getName();
        System.out.println("[Controller] 收到请求，当前线程: " + callerThread);

        // 调用异步方法——不会阻塞，立刻返回 CompletableFuture
        CompletableFuture<String> asyncResult = asyncService.doAsyncWork();

        // 顺便调同步方法做对比
        String syncResult = asyncService.doSyncWork();

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "Hello, Spring Boot 2.7!");

        // 等异步结果拿到后一起返回（教学用；生产中可以不等）
        String asyncVal = asyncResult.join();
        result.put("async", asyncVal);
        result.put("sync", syncResult);
        result.put("callerThread", callerThread);

        return result;
    }
}
```

---

## 三、验证方式

### 启动项目

```bash
cd /data/app/ai/ms/thread
mvn spring-boot:run
```

### 访问接口

浏览器打开 `http://localhost:10000/api/hello`

### 观察控制台

你会看到类似下面这样的输出：

```
[Controller] 收到请求，当前线程: http-nio-10000-exec-1
[异步任务] 执行线程: async-demo-1
[同步任务] 执行线程: http-nio-10000-exec-1
[异步任务] 完成! 线程: async-demo-1
```

**关键观察点：**


| 方法                       | 线程名                  | 说明                               |
| -------------------------- | ----------------------- | ---------------------------------- |
| Controller#hello()         | `http-nio-10000-exec-1` | Tomcat 请求处理线程                |
| AsyncService#doSyncWork()  | `http-nio-10000-exec-1` | 同一个线程，因为没加`@Async`       |
| AsyncService#doAsyncWork() | `async-demo-1`          | **不同线程！**你配置的线程池生效了 |

### 接口返回示例

```json
{
  "code": 200,
  "message": "Hello, Spring Boot 2.7!",
  "async": "异步任务完成，线程: async-demo-1",
  "sync": "同步任务完成，线程: http-nio-10000-exec-1",
  "callerThread": "http-nio-10000-exec-1"
}
```

---

## 四、避坑指南——3 个新手最容易踩的坑

### 坑一：方法必须是 public，且不能被同类内部调用

这是中毒率最高的坑。`@Async` 靠的是 **Spring AOP 代理**——Spring 给你的 Bean 包了一层代理对象，只有从"外面"调进来时，代理才能拦截到请求、切到线程池去。

**错误示范：**

```java
@Service
public class BadService {

    public String entry() {
        // 同类内部 this.doAsync() 调用，不走代理，@Async 完全无效！
        return this.doAsync();
    }

    @Async
    public String doAsync() { ... }
}
```

**正确做法：** 把 `@Async` 方法放在独立的 Service 里，通过 `@Autowired` 注入后调用。

### 坑二：线程池参数全用默认值，不自定义 Bean

如果不自定义 `ThreadPoolTaskExecutor`，Spring 会用 `SimpleAsyncTaskExecutor`。它**不限制线程数**，来一个任务就 new 一个线程，用完就丢。生产环境高并发下，线程数会飙到几千甚至上万，直接 OOM。

所以 **一定要自定义 `ThreadPoolTaskExecutor` Bean**，尤其是 `maxPoolSize` 和 `queueCapacity` 必须设上限。

### 坑三：异步方法返回 void，然后自己吞异常

```java
@Async
public void riskyWork() {
    throw new RuntimeException("完蛋"); // 调用方完全感知不到
}
```

`@Async` 方法返回 `void` 时，异常会静默"消失"。你有两个选择：

1. **返回 `CompletableFuture<T>`**（推荐）——调用方通过 `future.get()` / `future.join()` 能拿到异常
2. **配置全局 `AsyncUncaughtExceptionHandler`**——兜底捕获

---

## 五、项目最终结构

```
src/main/java/com/example/spdemo/
├── SpdemoApplication.java      # 加了 @EnableAsync
├── config/
│   └── ExecutorConfig.java     # 新增：线程池配置
├── controller/
│   └── HelloController.java    # 改造：注入 Service，打印线程名
└── service/
    └── AsyncService.java       # 新增：@Async 异步 + 同步对比方法
```

---

## 六、API 一览


| 接口             | 说明                                           |
| ---------------- | ---------------------------------------------- |
| `GET /api/hello` | 触发异步任务，控制台打印线程名，返回 JSON 结果 |

端口：`10000`（配置在 `application.properties` 中）
