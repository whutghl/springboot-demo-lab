package com.example.greeting;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ====================================================================
 * 【自动配置类 —— GreetingAutoConfiguration】
 *
 * 这是整个 Starter 的核心！
 *
 * 它的职责非常明确：
 *   当消费方项目引入本 Starter 后，Spring Boot 在启动时会自动
 *   扫描并加载本类，从而将 GreetingProperties 和 GreetingService
 *   自动注册到 Spring 容器中——消费方可以直接 @Autowired 使用。
 *
 * ─────────────────────────────────────────────────────────────────────
 * 【背后的魔法：SPI 机制 + @EnableAutoConfiguration】
 *
 * Spring Boot 是如何发现本类的？
 *
 * 步骤 1：@EnableAutoConfiguration 注解（在 @SpringBootApplication 中）
 *         会启用 spring.factories 的加载机制。
 *
 * 步骤 2：Spring Boot 会扫描所有 jar 包中的
 *         META-INF/spring.factories 文件。
 *
 * 步骤 3：在该文件中，key 为
 *         org.springframework.boot.autoconfigure.EnableAutoConfiguration
 *         value 列出了所有自动配置类的全限定名。
 *         多个类时，以逗号分隔
 *         org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
 *      com.example.greeting.GreetingAutoConfiguration,\
 *      com.example.greeting.AnotherAutoConfiguration,\
 *      com.example.greeting.ThirdAutoConfiguration     
 * 
 * 步骤 4：Spring Boot 读取到 value = com.example.greeting.GreetingAutoConfiguration
 *         后，就会加载这个类，处理其 @Bean / @Configuration 等内容。
 *
 * 这种通过 META-INF/spring.factories 注册服务的机制，就是
 * Java 原生的 SPI（Service Provider Interface）思想。
 * Spring Boot 将其发扬光大，成为 Starter 的标准入口。
 * ─────────────────────────────────────────────────────────────────────
 *
 * 【注解详解】
 *
 * @Configuration
 *   声明这是一个 Spring 配置类，相当于 XML 中的 <beans> 标签。
 *   内部可以包含 @Bean 方法，Spring 会调用这些方法创建 Bean。
 *
 * @EnableConfigurationProperties(GreetingProperties.class)
 *   激活 @ConfigurationProperties 绑定功能。
 *   只有加上这个注解，GreetingProperties 类上的
 *   @ConfigurationProperties(prefix = "mygreeting") 才会生效，
 *   Spring 才会将 application.properties 中的值注入到该类的字段中。
 *
 * @ConditionalOnMissingBean(GreetingService.class)
 *   这是一个条件装配注解，含义是："仅在容器中不存在 GreetingService
 *   类型的 Bean 时，才执行下面的方法创建 Bean"。
 *
 *   设计意图：
 *     如果消费方已经自己定义了一个 GreetingService Bean（比如为了
 *     定制行为），那么 Starter 就不应该覆盖它。这体现了"约定优于配置"
 *     和"尊重用户自定义"的原则。
 *
 *   类似的条件注解还有：
 *     - @ConditionalOnClass：当某个类在 classpath 上存在时
 *     - @ConditionalOnProperty：当某个配置项存在/等于某值时
 *     - @ConditionalOnBean：当某个 Bean 已存在时
 *     - @ConditionalOnMissingClass：当某个类不存在时
 * ====================================================================
 */
@Configuration
@EnableConfigurationProperties(GreetingProperties.class)
public class GreetingAutoConfiguration {

    // ================================================================
    // 构造方法 —— 本配置类不需要任何依赖，因此保持默认无参构造。
    // ================================================================

    /**
     * ================================================================
     * 创建 GreetingService Bean 的方法
     *
     * 工作流程：
     * 1. Spring 调用本方法时，会自动将 GreetingProperties 作为参数传入
     *    （方法参数的自动注入由 Spring 容器完成）。
     * 2. 方法内部手动 new 一个 GreetingService 实例，传入 properties。
     * 3. 返回的实例被注册到 Spring 容器中，成为可被 @Autowired 的 Bean。
     *
     * @ConditionalOnMissingBean：
     *   只有容器中还没有 GreetingService Bean 时才会执行该方法。
     *   这是"兜底"逻辑——如果用户自己定义了，那就用用户的。
     *
     * @Bean：
     *   告诉 Spring："这个方法会返回一个对象，请将它注册为 Bean。"
     *   默认的 Bean 名称 = 方法名（greetingService）。
     *   可以通过 @Bean(name = "xxx") 自定义名称。
     * ================================================================
     */
    @Bean
    @ConditionalOnMissingBean(GreetingService.class)
    public GreetingService greetingService( propertieGreetingPropertiess) {

        // ----------------------------------------------------------------
        // 这里可以写更复杂的初始化逻辑，比如：
        // - 根据 properties 中的某个开关决定是否创建
        // - 创建前做一些校验（如 prefix 不能为空）
        // - 打印日志，提示 Starter 已生效
        //
        // 但本示例力求最简，直接 new 对象。
        // ----------------------------------------------------------------
        return new GreetingService(properties);
    }
}