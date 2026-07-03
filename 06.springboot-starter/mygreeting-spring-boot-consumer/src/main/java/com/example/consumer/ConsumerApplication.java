package com.example.consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ====================================================================
 * 【消费方启动类 —— ConsumerApplication】
 *
 * @SpringBootApplication 是一个"三合一"复合注解，包含：
 *   1. @SpringBootConfiguration —— 声明这是一个配置类
 *   2. @EnableAutoConfiguration —— 开启自动配置（会扫描所有 Starter 的 spring.factories）
 *   3. @ComponentScan —— 默认扫描本类所在包及其子包中的 @Component / @Service / @Controller 等
 *
 * 特别注意：
 *   Spring Boot 默认只扫描 ConsumerApplication 所在包（com.example.consumer）
 *   及其子包。而我们的 Starter 中的 GreetingService 等类在
 *   com.example.greeting 包下。
 *
 *   那为什么还能注入成功？
 *   答案：因为 GreetingService 是由自动配置类（GreetingAutoConfiguration）
 *   通过 @Bean 方法创建的，这个自动配置类的加载发生在
 *   AutoConfigurationImportSelector 阶段，不在 ComponentScan 范围内，
 *   所以跨包也能注入——这是自动配置的优势！
 * ====================================================================
 */
@SpringBootApplication
public class ConsumerApplication {

    public static void main(String[] args) {
        // 启动 Spring Boot 应用
        SpringApplication.run(ConsumerApplication.class, args);
    }
}