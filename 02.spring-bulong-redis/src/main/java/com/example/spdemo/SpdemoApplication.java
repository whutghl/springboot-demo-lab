package com.example.spdemo;

import com.example.spdemo.redisbloom.RedisBloomService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SpdemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpdemoApplication.class, args);
    }

    /**
     * 应用启动后自动初始化布隆过滤器
     * 
     * 教学说明：
     * ApplicationRunner 在 Spring 容器完全启动后执行，
     * 此时 RedisTemplate 已经完成自动配置，可以安全使用。
     * 
     * 这模拟了真实场景：系统启动时，将数据库中已有的数据ID
     * 批量预热到布隆过滤器中。
     */
    @Bean
    public ApplicationRunner bloomFilterInitializer(RedisBloomService redisBloomService) {
        return args -> {
            System.out.println("=== 开始初始化布隆过滤器 ===");
            redisBloomService.initBloomFilter();
            System.out.println("=== 布隆过滤器初始化完成 ===");
            System.out.println("已预填演示数据: 1001, 1002, 1003, 1004, 1005");
        };
    }
}
