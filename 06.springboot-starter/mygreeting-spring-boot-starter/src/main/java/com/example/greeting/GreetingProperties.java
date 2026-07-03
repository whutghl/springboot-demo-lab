package com.example.greeting;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * ====================================================================
 * 【配置属性类 —— GreetingProperties】
 *
 * 作用：
 *   将 application.properties / application.yml 中以 "mygreeting" 前缀
 *   开头的配置项，自动绑定到本类的字段上。
 *
 * 典型用法（在消费方 application.properties 中）：
 *   mygreeting.prefix=你好
 *   mygreeting.suffix=！
 *
 * 设计理念：
 *   Starter 应当让使用者能通过配置文件自定义行为，而非硬编码。
 *   @ConfigurationProperties 正是 Spring Boot 为"外部化配置"提供
 *   的最强大机制——它省去了手动读取 Environment 的繁琐代码。
 *
 * 学习要点：
 * 1. @ConfigurationProperties(prefix = "xxx")
 *    声明这是一个配置绑定类，prefix 指定了属性前缀。
 *    Spring Boot 会在启动时将 Environment 中所有以 prefix 开头的
 *    属性，通过 setter 或直接字段注入的方式赋值给本类。
 *
 * 2. 必须提供 getter / setter（或使用 @Data 注解，但为了让初级读者
 *    看到完整机制，这里手动编写）。
 *
 * 3. 本类会被自动配置类（GreetingAutoConfiguration）注入使用。
 * ====================================================================
 */
@ConfigurationProperties(prefix = "mygreeting")
public class GreetingProperties {

    // ================================================================
    // 字段 1：prefix —— 问候语前缀
    // 默认值："Hello"（英文），用户可通过 mygreeting.prefix 覆盖
    // ================================================================
    private String prefix = "Hello";

    // ================================================================
    // 字段 2：suffix —— 问候语后缀
    // 默认值："!"，用户可通过 mygreeting.suffix 覆盖
    // ================================================================
    private String suffix = "!";

    // ================================================================
    // Getter / Setter
    // Spring Boot 通过 setter 注入配置值，
    // 其他 Bean 通过 getter 读取配置值。
    // ================================================================

    public String getPrefix() {
        return prefix;
    }

    /**
     * 设置前缀。Spring Boot 在绑定时会调用此方法。
     * @param prefix 前缀字符串
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }
}