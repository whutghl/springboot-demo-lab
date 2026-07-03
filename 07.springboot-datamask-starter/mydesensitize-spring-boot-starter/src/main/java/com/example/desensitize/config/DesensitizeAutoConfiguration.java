package com.example.desensitize.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * ====================================================================
 * 【自动配置类 —— DesensitizeAutoConfiguration】
 *
 * 这是整个 Starter 的核心入口！
 *
 * 职责：
 *   当消费方项目引入本 Starter 后，Spring Boot 在启动时会自动
 *   扫描并加载本类，从而将 DesensitizeProperties 注册到 Spring
 *   容器中，使 @ConfigurationProperties 绑定生效。
 *
 * ─────────────────────────────────────────────────────────────────────
 * 【为什么没有 @Bean 方法？】
 *
 * 本 Starter 的核心能力是通过 @DataMask 注解 + Jackson 的
 * ContextualSerializer 机制实现的，不需要额外的 Bean 实例。
 * Jackson 的 ObjectMapper 在启动时已经扫描了所有 @JsonSerializer
 * 注解，DataMaskSerializer 会自动被注册到 Jackson 的序列化器注册表中。
 *
 * 本类的核心作用：
 *   1. @EnableConfigurationProperties → 激活 DesensitizeProperties 的配置绑定
 *   2. 使 Spring Boot 的 SPI 机制能够发现并加载此 Starter
 *
 * ─────────────────────────────────────────────────────────────────────
 * 【核心原理】
 *
 * @EnableConfigurationProperties(DesensitizeProperties.class)
 *   激活 @ConfigurationProperties 绑定功能。
 *   只有加上这个注解，DesensitizeProperties 类上的
 *   @ConfigurationProperties(prefix = "mydesensitize") 才会生效，
 *   Spring 才会将 application.properties 中的值注入到该类的字段中。
 *
 * 条件注解虽然没有显式使用，但设计上遵循：
 *   - 用户随时可以自定义 Jackson 的序列化行为
 *   - Starter 不强制注册任何非必要的 Bean
 * ====================================================================
 */
@Configuration
@EnableConfigurationProperties(DesensitizeProperties.class)
public class DesensitizeAutoConfiguration {

    // ================================================================
    // 本配置类没有任何 @Bean 方法
    //
    // 原因：@DataMask 注解的序列化能力是由 Jackson 的
    //       ContextualSerializer 机制自动完成的，无需手动注册 Bean。
    //
    // 本类仅用于：
    //   1. 激活 @ConfigurationProperties 绑定（DesensitizeProperties）
    //   2. 作为 SPI 入口，让 Spring Boot 发现本 Starter
    // ================================================================

    // 如果将来需要提供编程式脱敏服务 Bean，可以在此添加：
    //
    // @Bean
    // @ConditionalOnMissingBean(DesensitizeService.class)
    // public DesensitizeService desensitizeService(DesensitizeProperties properties) {
    //     return new DesensitizeService(properties);
    // }
}