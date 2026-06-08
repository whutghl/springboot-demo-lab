package com.example.spdemo;

import com.google.common.hash.BloomFilter;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Spring Boot 启动类
 *
 * 启动时通过 CommandLineRunner 向布隆过滤器预填演示数据，
 * 模拟「系统启动时从数据库加载已有用户ID到布隆过滤器」的场景。
 *
 * 在真实的防缓存穿透方案中，这一步通常是在服务启动时：
 *   1. 从数据库查出所有合法 key（如用户ID、商品ID）
 *   2. 全部写入布隆过滤器
 *   3. 之后查询请求先过布隆过滤器，false 则直接拦截
 */
@SpringBootApplication
public class SpdemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpdemoApplication.class, args);
    }

    /**
     * 启动时预填 5 条演示数据（user_1 ~ user_5）
     *
     * 这一步对应布隆过滤器的「冷启动 / 预热」概念：
     *   布隆过滤器本身不存储原始数据，只记录"哪些 key 曾经被加入过"的指纹信息。
     *   所以必须有一个预热过程，把已知的合法数据灌进去，
     *   否则空的布隆过滤器对所有查询都会返回 true（无意义）。
     */
    @Bean
    public CommandLineRunner preloadBloomFilter(BloomFilter<String> bloomFilter) {
        return args -> {
            for (int i = 1; i <= 5; i++) {
                String user = "user_" + i;
                bloomFilter.put(user);
                System.out.println("[布隆过滤器预热] 已添加: " + user);
            }
            System.out.println("[布隆过滤器预热] 完成，共 5 条数据");
        };
    }
}
