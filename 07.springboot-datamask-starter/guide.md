# MyDesensitize Spring Boot Starter — 注解驱动的数据脱敏 Starter

> 适用版本：Spring Boot 2.7.x（基于 Java 8）
> 作者：用于教学的自定义 Starter 示例

---

## 目录

- [MyDesensitize Spring Boot Starter](#mydesensitize-spring-boot-starter--注解驱动的数据脱敏-starter)
  - [目录](#目录)
  - [1. 项目概览](#1-项目概览)
  - [2. 文件结构](#2-文件结构)
  - [3. 核心原理图解](#3-核心原理图解)
  - [4. 逐模块深度解析](#4-逐模块深度解析)
    - [4.1 脱敏类型枚举：MaskType](#41-脱敏类型枚举masktype)
    - [4.2 脱敏注解：@DataMask](#42-脱敏注解datamask)
    - [4.3 核心序列化器：DataMaskSerializer](#43-核心序列化器datamaskserializer)
      - [ContextualSerializer 机制](#contextualserializer-机制)
    - [4.4 脱敏工具类：DesensitizeUtil](#44-脱敏工具类desensitizeutil)
    - [4.5 配置属性类：DesensitizeProperties](#45-配置属性类desensitizeproperties)
    - [4.6 自动配置类：DesensitizeAutoConfiguration](#46-自动配置类desensitizeautoconfiguration)
  - [5. 消费方如何使用](#5-消费方如何使用)
    - [5.1 添加依赖（pom.xml）](#51-添加依赖pomxml)
    - [5.2 在实体类字段上加注解](#52-在实体类字段上加注解)
    - [5.3 直接返回实体，自动脱敏](#53-直接返回实体自动脱敏)
  - [6. 运行步骤](#6-运行步骤)
    - [6.1 安装 Starter 到本地仓库](#61-安装-starter-到本地仓库)
    - [6.2 启动消费方应用](#62-启动消费方应用)
    - [6.3 验证](#63-验证)
  - [7. 各类型脱敏效果一览](#7-各类型脱敏效果一览)
  - [8. 扩展练习](#8-扩展练习)

---

## 1. 项目概览

本项目由 **两个 Maven 模块** 组成：

| 模块                                   | 作用           | 关键产出                                 |
| -------------------------------------- | -------------- | ---------------------------------------- |
| `mydesensitize-spring-boot-starter`  | Starter 提供方 | `@DataMask` 注解 + 自动脱敏序列化器    |
| `mydesensitize-spring-boot-consumer` | Starter 消费方 | 引用 Starter，通过 REST 接口展示脱敏效果 |

**一句话总结：**
消费者在实体类的字段上标注 `@DataMask(type = MaskType.PHONE)`，当该实体被 Jackson 序列化为 JSON 时，敏感字段会自动脱敏。

---

## 2. 文件结构

```
phone-stater/
├── mydesensitize-spring-boot-starter/         # ★ Starter 模块
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/example/desensitize/
│       │   ├── annotation/
│       │   │   ├── MaskType.java              # 脱敏类型枚举
│       │   │   └── DataMask.java              # ★ 核心注解
│       │   ├── serializer/
│       │   │   └── DataMaskSerializer.java    # ★ 核心序列化器
│       │   ├── service/
│       │   │   └── DesensitizeUtil.java       # 脱敏工具类
│       │   └── config/
│       │       ├── DesensitizeProperties.java # 配置属性
│       │       └── DesensitizeAutoConfiguration.java  # 自动配置
│       └── resources/META-INF/
│           └── spring.factories               # ★ SPI 注册文件
│
├── mydesensitize-spring-boot-consumer/         # 消费方模块
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/example/consumer/
│       │   ├── ConsumerApplication.java       # 启动类
│       │   ├── entity/
│       │   │   └── UserVO.java               # ★ 使用 @DataMask 的实体
│       │   └── controller/
│       │       └── UserController.java        # REST 控制器
│       └── resources/
│           └── application.properties         # 配置 mydesensitize.*
│
└── guide.md                                   # 本文档
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
│    │                 └── 扫描META-INF/spring.factories                   │
│    │                       → 发现 DesensitizeAutoConfiguration           │
│    │                                                                     │
│    ├── Jackson 自动配置 (JacksonAutoConfiguration)                        │
│    │     └── ObjectMapper 扫描所有 @JsonSerialize 注解                   │
│    │           └── 发现 DataMaskSerializer 实现了 ContextualSerializer   │
│    │                                                                     │
│    └── @ComponentScan                                                    │
│          └── 扫描 @RestController → UserController                       │
│                                                                         │
│  ── 请求到达 ──                                                          │
│                                                                         │
│  GET /user → UserController.getUser()                                    │
│    └── return new UserVO("张三", "13812345678", ...)                     │
│          └── Jackon 序列化 UserVO                                         │
│                ├── id: Long        → 普通序列化 → "id": 1                │
│                ├── name: String    → 发现@DataMask(NAME)                 │
│                │     └── createContextual() → 获取 MaskType.NAME          │
│                │     └── serialize() → maskName("张三") → "张*"           │
│                ├── phone: String   → 发现@DataMask(PHONE)                │
│                │     └── serialize() → maskPhone("13812345678")          │
│                │                                    → "138****5678"       │
│                └── ...                                                   │
│                                                                         │
│  返回 JSON：                                                              │
│  {                                                                       │
│    "id": 1,                                                              │
│    "name": "张*",                                                         │
│    "phone": "138****5678",                                                │
│    ...                                                                    │
│  }                                                                       │
└─────────────────────────────────────────────────────────────────────────┘
```

**关键流程说明：**

1. **SPI 发现**：Spring Boot 启动时读取 `spring.factories`，找到 `DesensitizeAutoConfiguration`
2. **Jackson 初始化**：Jackson 的 `ObjectMapper` 扫描所有 `@JsonSerialize` 注解，发现 `DataMaskSerializer` 实现了 `ContextualSerializer` 接口
3. **上下文构建**：当序列化带 `@DataMask` 注解的字段时，Jackson 调用 `createContextual()` 方法提取注解中的脱敏类型
4. **自动脱敏**：实际序列化时，根据脱敏类型（PHONE/NAME/ID_CARD/EMAIL/ADDRESS）执行对应的脱敏策略

---

## 4. 逐模块深度解析

### 4.1 脱敏类型枚举：MaskType

```java
public enum MaskType {
    PHONE,    // 手机号：13812345678 → 138****5678
    NAME,     // 姓名：张三 → 张*，欧阳修 → 欧阳*
    ID_CARD,  // 身份证：110101199001011234 → 110101****1234
    EMAIL,    // 邮箱：zhangsan@example.com → z******@example.com
    ADDRESS,  // 地址：北京市朝阳区... → 北京市朝阳区****
    CUSTOM    // 自定义（预留）
}
```

**设计要点：**

- 每个枚举值对应一种脱敏规则，便于扩展
- `CUSTOM` 类型预留用于用户通过配置自定义规则

### 4.2 脱敏注解：@DataMask

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@JacksonAnnotationsInside
@JsonSerialize(using = DataMaskSerializer.class)
public @interface DataMask {
    MaskType type() default MaskType.PHONE;
}
```

**注解说明：**

| 元注解                          | 作用                                        |
| ------------------------------- | ------------------------------------------- |
| `@Retention(RUNTIME)`         | 运行时保留，Jackson 运行时可通过反射读取    |
| `@Target(FIELD)`              | 只能标注在字段上                            |
| `@JacksonAnnotationsInside`   | 标记这是一个 Jackson 组合注解               |
| `@JsonSerialize(using = ...)` | 指定该字段使用`DataMaskSerializer` 序列化 |

### 4.3 核心序列化器：DataMaskSerializer

#### ContextualSerializer 机制

这是本 Starter 最核心的技术点。`ContextualSerializer` 是 Jackson 提供的一个接口，允许序列化器在初始化时"感知"字段上的注解信息。

```java
public class DataMaskSerializer 
    extends JsonSerializer<String> 
    implements ContextualSerializer {

    private MaskType type;

    @Override
    public JsonSerializer<?> createContextual(
            SerializerProvider prov, BeanProperty property) {

        // 获取字段上的 @DataMask 注解
        DataMask annotation = property.getAnnotation(DataMask.class);
        MaskType maskType = annotation.type();

        // 返回带类型的序列化器实例
        return new DataMaskSerializer(maskType);
    }

    @Override
    public void serialize(String value, JsonGenerator gen, 
                          SerializerProvider prov) throws IOException {
        // 根据 type 执行脱敏
        String masked = applyMask(value, this.type);
        gen.writeString(masked);
    }
}
```

**执行流程：**

```
DataMaskSerializer 实例化
  └─> createContextual() 被调用
        ├─> 获取字段上的 @DataMask(type = PHONE)
        └─> 创建新的 DataMaskSerializer(PHONE)

序列化时：
  └─> serialize("13812345678", gen, prov)
        ├─> applyMask("13812345678", PHONE)
        │     └─> maskPhone("13812345678")
        │           └─> "138" + "****" + "5678"
        └─> gen.writeString("138****5678")
```

**脱敏规则一览：**

| 类型            | 规则                      | 示例                                                 |
| --------------- | ------------------------- | ---------------------------------------------------- |
| `PHONE`       | 保留前3位 + **** + 后4位  | `13812345678` → `138****5678`                   |
| `NAME` (2字)  | 保留第1位 + *             | `张三` → `张*`                                  |
| `NAME` (3字)  | 保留第1位 + **            | `李小明` → `李**`                               |
| `NAME` (4字+) | 保留前2位 + 剩余*         | `欧阳修` → `欧阳*`                              |
| `ID_CARD`     | 保留前6位 + **** + 后4位  | `110101199001011234` → `110101****1234`         |
| `EMAIL`       | 保留首字符 + ****@ + 域名 | `zhangsan@example.com` → `z******@example.com`  |
| `ADDRESS`     | 保留前6位 + 剩余*         | `北京市朝阳区建国路88号` → `北京市朝阳区******` |

### 4.4 脱敏工具类：DesensitizeUtil

提供一组静态工具方法，用于在非序列化场景下手动脱敏（如日志输出）。

```java
// 手动脱敏示例
String maskedPhone = DesensitizeUtil.desensitize("13812345678", MaskType.PHONE);
// 结果：138****5678

// 或直接调用具体方法
String maskedName = DesensitizeUtil.maskName("张三");
// 结果：张*
```

### 4.5 配置属性类：DesensitizeProperties

```java
@ConfigurationProperties(prefix = "mydesensitize")
public class DesensitizeProperties {
    private char maskChar = '*';        // 脱敏符号
    private int phoneFrontKeep = 3;     // 手机号保留前几位
    private int phoneBackKeep = 4;      // 手机号保留后几位
    private int nameSurnameKeep = 1;    // 姓名保留姓的位数
    private int idCardFrontKeep = 6;    // 身份证保留前几位
    private int idCardBackKeep = 4;     // 身份证保留后几位
    private int addressFrontKeep = 6;   // 地址保留前几位
}
```

消费方可通过 `application.properties` 自定义脱敏行为：

```properties
mydesensitize.mask-char=#
mydesensitize.phone-front-keep=2
mydesensitize.phone-back-keep=4
```

### 4.6 自动配置类：DesensitizeAutoConfiguration

```java
@Configuration
@EnableConfigurationProperties(DesensitizeProperties.class)
public class DesensitizeAutoConfiguration {
    // 本类没有 @Bean 方法
    // @DataMask 注解的序列化由 Jackson 的 ContextualSerializer 自动完成
    // 本类仅用于激活 @ConfigurationProperties 绑定和作为 SPI 入口
}
```

与传统的 Greeting Starter 不同，本 Starter 的自动配置类中**没有任何 @Bean 方法**，因为脱敏的核心能力是通过注解 + Jackson 序列化器实现的，不需要注册额外的 Bean 到 Spring 容器。

---

## 5. 消费方如何使用

### 5.1 添加依赖（pom.xml）

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>mydesensitize-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 5.2 在实体类字段上加注解

```java
public class UserVO {
    private Long id;

    @DataMask(type = MaskType.NAME)
    private String name;

    @DataMask(type = MaskType.PHONE)
    private String phone;

    @DataMask(type = MaskType.ID_CARD)
    private String idCard;

    @DataMask(type = MaskType.EMAIL)
    private String email;

    @DataMask(type = MaskType.ADDRESS)
    private String address;
}
```

### 5.3 直接返回实体，自动脱敏

```java
@RestController
public class UserController {

    @GetMapping("/user")
    public UserVO getUser() {
        return new UserVO(1L, "张三", "13812345678",
            "110101199001011234", "zhangsan@example.com",
            "北京市朝阳区建国路88号");
    }
}
```

**不需要：**

- 不需要手动调用脱敏方法
- 不需要在 Controller 中做任何额外处理
- 不需要配置 Jackson 的 ObjectMapper

---

## 6. 运行步骤

### 6.1 安装 Starter 到本地仓库

```bash
cd mydesensitize-spring-boot-starter
mvn install -DskipTests
```

### 6.2 启动消费方应用

```bash
cd ../mydesensitize-spring-boot-consumer
mvn spring-boot:run
```

### 6.3 验证

```bash
# 查看单个用户
curl http://localhost:8080/user

# 查看用户列表
curl http://localhost:8080/users
```

**预期输出（单个用户）：**

```json
{
    "id": 1,
    "name": "张*",
    "phone": "138****5678",
    "idCard": "110101********1234",
    "email": "z*******@example.com",
    "address": "北京市朝阳区******"
}
```

---

## 7. 各类型脱敏效果一览

| 字段        | 原始值                 | 脱敏后                     |
| ----------- | ---------------------- | -------------------------- |
| 姓名 (2字)  | 张三                   | 张\*                       |
| 姓名 (3字)  | 李小明                 | 李\*\*                     |
| 姓名 (复姓) | 欧阳修                 | 欧阳\*                     |
| 手机号      | 13812345678            | 138\*\*\*\*5678            |
| 身份证      | 110101199001011234     | 110101\*\*\*\*\*\*\*\*1234 |
| 邮箱        | zhangsan@example.com   | z\*\*\*\*\*\*@example.com  |
| 地址        | 北京市朝阳区建国路88号 | 北京市朝阳区\*\*\*\*\*\*   |

---

## 8. 扩展练习

学完本示例后，你可以尝试以下扩展来加深理解：

1. **添加新的脱敏类型**：在 `MaskType` 中增加 `BANK_CARD`（银行卡号），在 `DataMaskSerializer` 中添加对应的 `maskBankCard()` 方法。
2. **自定义脱敏符号**：通过 `application.properties` 中的 `mydesensitize.mask-char` 修改脱敏符号（如改为 `#`）。
3. **实现 @ConditionalOnProperty**：在自动配置类上添加 `@ConditionalOnProperty(prefix = "mydesensitize", name = "enabled", matchIfMissing = true)`，允许用户通过配置完全关闭脱敏。
4. **支持反序列化**：实现 `JsonDeserializer` + `ContextualDeserializer`，使 `@DataMask` 注解在反序列化接收请求时也能做脱敏处理。
5. **集成日志脱敏**：使用 `DesensitizeUtil` 工具类在日志输出中对敏感信息进行脱敏，避免敏感数据泄露到日志文件。

---

*Happy Coding! 🚀*
