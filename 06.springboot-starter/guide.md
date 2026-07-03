# MyGreeting Spring Boot Starter — 手把手运行原理与配置详解

> 适用版本：Spring Boot 2.7.x（基于 Java 8）  
> 作者：用于教学的自定义 Starter 示例

---

## 目录

- [MyGreeting Spring Boot Starter — 手把手运行原理与配置详解](#mygreeting-spring-boot-starter--手把手运行原理与配置详解)
  - [目录](#目录)
  - [1. 项目概览](#1-项目概览)
  - [2. 文件结构](#2-文件结构)
  - [3. 核心原理图解](#3-核心原理图解)
  - [4. 逐模块深度解析](#4-逐模块深度解析)
    - [4.1 配置属性类：GreetingProperties](#41-配置属性类greetingproperties)
    - [4.2 服务类：GreetingService](#42-服务类greetingservice)
    - [4.3 自动配置类：GreetingAutoConfiguration](#43-自动配置类greetingautoconfiguration)
      - [源码](#源码)
      - [实验对比：有 vs 没有 `@ConditionalOnMissingBean`](#实验对比有-vs-没有-conditionalonmissingbean)
      - [逻辑类比：智能锁](#逻辑类比智能锁)
    - [4.4 SPI 注册文件：spring.factories](#44-spi-注册文件springfactories)
  - [5. 消费方如何使用](#5-消费方如何使用)
    - [5.1 添加依赖（pom.xml）](#51-添加依赖pomxml)
    - [5.2 配置属性（application.properties）](#52-配置属性applicationproperties)
    - [5.3 注入并使用（任何 @Component/@Service/@Controller）](#53-注入并使用任何-componentservicecontroller)
  - [6. 运行步骤](#6-运行步骤)
    - [6.1 安装 Starter 到本地仓库](#61-安装-starter-到本地仓库)
    - [6.2 启动消费方应用](#62-启动消费方应用)
    - [6.3 验证](#63-验证)
  - [7. 扩展练习](#7-扩展练习)

---

## 1. 项目概览

本项目由 **两个 Maven 模块** 组成：

| 模块 | 作用 | 关键产出 |
|------|------|----------|
| `mygreeting-spring-boot-starter` | Starter 提供方 | 自动装配的 `GreetingService` |
| `mygreeting-spring-boot-consumer` | Starter 消费方 | 引用 Starter，通过 REST 接口展示效果 |

**一句话总结：**  
消费者在 `pom.xml` 中添加 Starter 依赖，在 `application.properties` 中配置 `mygreeting.prefix` 和 `mygreeting.suffix`，然后就可以在任何地方 `@Autowired` 注入 `GreetingService` 来获得定制化的问候语。

---

## 2. 文件结构

```
springboot-starter/
├── mygreeting-spring-boot-starter/        # ★ Starter 模块
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/example/greeting/
│       │   ├── GreetingProperties.java     # 配置绑定（prefix = "mygreeting"）
│       │   ├── GreetingService.java        # 核心服务
│       │   └── GreetingAutoConfiguration.java  # ★ 自动配置类
│       └── resources/META-INF/
│           └── spring.factories            # ★ SPI 注册文件（入口）
│
├── mygreeting-spring-boot-consumer/        # 消费方模块
│   ├── pom.xml                             # 依赖 mygreeting-spring-boot-starter
│   └── src/main/
│       ├── java/com/example/consumer/
│       │   ├── ConsumerApplication.java    # 启动类
│       │   └── GreetingController.java     # REST 控制器
│       └── resources/
│           └── application.properties      # 配置 mygreeting.prefix/suffix
│
└── guide.md                                # 本文档
```

---

## 3. 核心原理图解

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        Spring Boot 启动流程                              │
│                                                                         │
│  @SpringBootApplication                                                  │
│    ├── @EnableAutoConfiguration                                          │
│    │     └── AutoConfigurationImportSelector                             │
│    │           └── SpringFactoriesLoader.loadFactoryNames()              │
│    │                 └── 扫描所有 jar 包的                               │
│    │                     META-INF/spring.factories 文件                  │
│    │                           │                                         │
│    │                           ▼                                         │
│    │              ┌──────────────────────────────┐                       │
│    │              │ spring.factories 内容:       │                       │
│    │              │ org.springframework.boot.    │                       │
│    │              │ autoconfigure.               │                       │
│    │              │ EnableAutoConfiguration =    │                       │
│    │              │ com.example.greeting.        │                       │
│    │              │ GreetingAutoConfiguration    │                       │
│    │              └───────────┬──────────────────┘                       │
│    │                          │                                          │
│    │                          ▼                                          │
│    │             加载 GreetingAutoConfiguration                          │
│    │               ├── @Configuration                                   │
│    │               ├── @EnableConfigurationProperties(GreetingProperties)│
│    │               │      └── 绑定 application.properties 中             │
│    │               │          以 "mygreeting" 前缀的配置                 │
│    │               └── @Bean greetingService(properties)                │
│    │                    └── new GreetingService(properties)              │
│    │                          │                                          │
│    │                          ▼                                          │
│    │              GreetingService Bean 注册到 Spring 容器                 │
│    │                                                                     │
│    └── @ComponentScan                                                    │
│          └── 扫描 @RestController → GreetingController                   │
│                └── @Autowired GreetingService  ← 从容器注入               │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

**关键流程说明：**

1. **SPI 发现（第 1 步）**：Spring Boot 启动时，`AutoConfigurationImportSelector` 读取 `spring.factories` 找到 `GreetingAutoConfiguration`。
2. **配置绑定（第 2 步）**：`@EnableConfigurationProperties` 激活 `GreetingProperties`，将 `application.properties` 中以 `mygreeting` 开头的属性注入到该对象的字段。
3. **Bean 创建（第 3 步）**：`@Bean` 方法创建 `GreetingService`，传入已绑定好值的 `properties` 对象。`@ConditionalOnMissingBean` 确保"用户自定义优先"。
4. **消费注入（第 4 步）**：`@ComponentScan` 扫描到 `GreetingController`，`@Autowired` 从容器中取出 `GreetingService` 完成注入。

---

## 4. 逐模块深度解析

### 4.1 配置属性类：GreetingProperties

```java
@ConfigurationProperties(prefix = "mygreeting")
public class GreetingProperties { ... }
```

**原理：**

- `@ConfigurationProperties(prefix = "mygreeting")` 告诉 Spring Boot：将所有以 `mygreeting.` 开头的配置项（如 `mygreeting.prefix`）绑定到本类的对应字段。
- 本质是调用了 `Environment.getProperty("mygreeting.prefix")` → 调用 `setPrefix("你好")`。
- 必须配合 `@EnableConfigurationProperties` 使用才能在自动配置类中生效。

**设计理念：**  
"外部化配置"（Externalized Configuration）是 12-Factor App 的核心原则之一。通过配置而非硬编码，同一份 Starter 可以部署到开发、测试、生产环境，仅需修改配置文件即可改变行为。

### 4.2 服务类：GreetingService

```java
public class GreetingService {
    public String greet(String name) {
        return prefix + ", " + name + suffix;
    }
}
```

**关键点：**

- **无任何 Spring 注解**——它只是一个普通的 POJO（Plain Old Java Object）。
- 它的创建完全交由 `GreetingAutoConfiguration` 的 `@Bean` 方法负责。
- 这样做的目的是：Starter 的"组件"和"装配逻辑"分离——组件就是普通 Java 类，装配逻辑由自动配置类集中管理。

### 4.3 自动配置类：GreetingAutoConfiguration

#### 源码

```java
@Configuration
@EnableConfigurationProperties(GreetingProperties.class)
public class GreetingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(GreetingService.class)
    public GreetingService greetingService(GreetingProperties properties) {
        return new GreetingService(properties);
    }
}
```

**核心注解详解：**

| 注解 | 作用 |
|------|------|
| `@Configuration` | 声明本类是一个 Spring 配置类（类似 XML `<beans>`），内部 `@Bean` 方法会被处理 |
| `@EnableConfigurationProperties` | 激活 `GreetingProperties` 的配置绑定功能 |
| `@Bean` | 告诉 Spring：方法返回值（`GreetingService` 实例）应注册为一个 Bean |
| `@ConditionalOnMissingBean` | **条件装配**：仅当容器中尚无 `GreetingService` 类型的 Bean 时，才创建 |

**关于 `@ConditionalOnMissingBean` 的设计哲学——"退让机制"：**

`@ConditionalOnMissingBean` 是 Spring Boot 条件装配体系中最常用的注解之一。可以用一句话概括它的行为：

> **"如果你（用户）已经定义了这个 Bean，我就不插手；如果你没定义，我来兜底。"**

换言之，Starter 的自动配置始终是**可被覆盖的默认值**，用户的显式定义优先级更高。

#### 实验对比：有 vs 没有 `@ConditionalOnMissingBean`

假设公司有 10 个微服务都用了本 Starter，但其中 **服务 A** 需要自定义问候逻辑（全大写 + 日志）。

**❌ 实验 A：没有 `@ConditionalOnMissingBean` → 报错**

```java
// Starter 中——没有条件注解
@Bean
public GreetingService greetingService(GreetingProperties p) {
    return new GreetingService(p);
}
```

服务 A 开发者写自定义配置：
```java
@Configuration
public class MyConfig {
    @Bean
    public GreetingService greetingService() {
        return new GreetingService(new GreetingProperties()) {
            @Override
            public String greet(String name) {
                String r = super.greet(name).toUpperCase();
                System.out.println("[LOG] " + r);
                return r;
            }
        };
    }
}
```

**运行结果：**
```
The bean 'greetingService' could not be registered.
A bean with that name has already been defined in MyConfig
```
Spring 发现两个 `GreetingService` Bean → 无法抉择 → 抛 `BeanDefinitionOverrideException` → **启动失败**。

**✅ 实验 B：加上 `@ConditionalOnMissingBean` → 正常启动**

```java
// Starter 中——加了条件注解
@Bean
@ConditionalOnMissingBean(GreetingService.class)
public GreetingService greetingService(GreetingProperties p) {
    return new GreetingService(p);
}
```

**运行结果：**
```
→ 访问 /greet?name=world
→ 返回 "HELLO, WORLD！"
→ 控制台输出 [LOG] HELLO, WORLD！
```

`@ConditionalOnMissingBean` 检测到容器中**已经存在** `GreetingService` 类型的 Bean（来自 `MyConfig`），于是**跳过**自己的 `@Bean` 方法。用户的 Bean 被唯一保留，正常生效。

**其他 9 个服务**没有自定义 `GreetingService` → 条件成立 → Starter 创建默认 Bean → 一切正常。

#### 逻辑类比：智能锁

```
@ConditionalOnMissingBean 就像一把智能锁：
- 钥匙孔没人插 → 锁弹开（Starter 创建 Bean）
- 钥匙已经插着 → 锁保持不动（Starter 跳过，用户 Bean 生效）
```

这种"用户优先、Starter 兜底"的设计，体现了 **"约定优于配置"**（Convention over Configuration）和**可覆盖性**（Overridability）两大原则：

> **约定：** 你引入 Starter，它帮你配好一切——开箱即用。  
> **覆盖：** 你随时可以定义同类型 Bean，Starter 自动退让——你的规则优先。

**常见的条件注解一览：**

| 注解 | 触发条件 |
|------|---------|
| `@ConditionalOnClass` | classpath 中存在指定类 |
| `@ConditionalOnMissingClass` | classpath 中不存在指定类 |
| `@ConditionalOnBean` | 容器中已存在指定 Bean |
| `@ConditionalOnMissingBean` | 容器中不存在指定 Bean |
| `@ConditionalOnProperty` | 配置文件中存在指定属性（可控制开关） |
| `@ConditionalOnExpression` | SpEL 表达式结果为 true |
| `@ConditionalOnWebApplication` | 当前是 Web 应用环境 |

### 4.4 SPI 注册文件：spring.factories

**文件位置：** `META-INF/spring.factories`

```
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
com.example.greeting.GreetingAutoConfiguration
```

**SPI 机制详解：**

SPI（Service Provider Interface）是 Java 内置的一种服务发现机制，最早用于 JDBC 驱动加载。Spring Boot 对其进行了改造和增强，形成了 `SpringFactoriesLoader`。

**启动时的精确调用链：**

```
SpringApplication.run()
  └─> refreshContext()
       └─> AbstractApplicationContext.refresh()
            └─> invokeBeanFactoryPostProcessors()
                 └─> AutoConfigurationImportSelector
                      └─> selectImports()
                           └─> SpringFactoriesLoader.loadFactoryNames()
                                └─> 遍历 classpath 下所有
                                    META-INF/spring.factories
                                    读取 key=
                                    EnableAutoConfiguration 的所有 value
                                    去重、排序后返回类名列表
                                      └─> 逐一加载 Configuration 类
```

**为什么必须用 `spring.factories` 而不能用 `@ComponentScan`？**

因为 `GreetingAutoConfiguration` 位于 `com.example.greeting` 包，而消费方的启动类在 `com.example.consumer` 包。默认的 `@ComponentScan` 只扫描 `com.example.consumer` 及其子包，无法跨包扫描。`spring.factories` 绕过了包扫描的限制，实现了跨 jar 包的服务发现——这是 Starter 能够"开箱即用"的根本原因。

> **Spring Boot 2.7.x 补充说明：**  
> 从 Spring Boot 2.7 开始，官方推荐使用 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 文件替代 `spring.factories`，但 `spring.factories` 在 2.7.x 中仍然完全兼容。本示例使用传统方式以便于理解。

---

## 5. 消费方如何使用

### 5.1 添加依赖（pom.xml）

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>mygreeting-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 5.2 配置属性（application.properties）

```properties
mygreeting.prefix=你好
mygreeting.suffix=！
```

### 5.3 注入并使用（任何 @Component/@Service/@Controller）

```java
@Autowired
private GreetingService greetingService;

public void demo() {
    String msg = greetingService.greet("Spring Boot");
    // msg = "你好, Spring Boot！"
}
```

**整个过程无需：**

- 无需 `@Import` 任何类
- 无需 `@ComponentScan` 指定额外包路径
- 无需手动创建 `GreetingService` 实例

这就是 Starter 的价值——**"加依赖 + 写配置 = 开箱即用"**。

---

## 6. 运行步骤

### 6.1 安装 Starter 到本地仓库

```bash
cd mygreeting-spring-boot-starter
mvn install -DskipTests
```

### 6.2 启动消费方应用

```bash
cd ../mygreeting-spring-boot-consumer
mvn spring-boot:run
```

### 6.3 验证

浏览器或 curl 访问：

```
http://localhost:8080/greet?name=世界
```

返回：

```
你好, 世界！
```

修改 `application.properties` 中的 `mygreeting.prefix` 和 `mygreeting.suffix`，重启后可见效果。

---

## 7. 扩展练习

学完本示例后，你可以尝试以下扩展来加深理解：

1. **添加开关控制**：在 `GreetingProperties` 中增加 `enabled` 字段（默认 `true`），在 `GreetingAutoConfiguration` 上使用 `@ConditionalOnProperty(prefix = "mygreeting", name = "enabled", matchIfMissing = true)` 来完全控制 Starter 是否生效。

2. **增加 Starter 自动提示**：引入 `spring-boot-configuration-processor`（本示例已引入），编译后 IDE 会自动提示 `mygreeting.prefix` 和 `mygreeting.suffix`。

3. **支持 YAML 多环境**：在消费方创建 `application-dev.yml` 和 `application-prod.yml`，使用不同的问候语配置。

4. **实现 @ConditionalOnClass**：如果 `GreetingService` 依赖了某个第三方库（如 Jackson），可以在自动配置类上加 `@ConditionalOnClass(com.fasterxml.jackson.databind.ObjectMapper.class)`，仅当该库存在时才启用 Starter。

---

*Happy Coding! 🚀*