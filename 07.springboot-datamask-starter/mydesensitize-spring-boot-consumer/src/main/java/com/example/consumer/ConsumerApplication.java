package com.example.consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ====================================================================
 * 【消费方启动类 —— ConsumerApplication】
 *
 * 这是演示脱敏 Starter 的 Spring Boot 应用入口。
 *
 * 启动后访问：
 *   http://localhost:8080/user    → 查看单个用户脱敏效果
 *   http://localhost:8080/users   → 查看用户列表脱敏效果
 * ====================================================================
 */
@SpringBootApplication
public class ConsumerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConsumerApplication.class, args);
    }
}