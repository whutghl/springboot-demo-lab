package com.example.spdemo.config;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;

/**
 * 布隆过滤器配置类
 *
 * 布隆过滤器的核心概念：
 *   1. 位数组（bit array）—— 底层是一串二进制位，初始全为 0
 *   2. 哈希函数（hash functions）—— 把元素映射到位数组中的多个位置
 *   3. 期望插入数（expectedInsertions）—— 预计要放多少元素，决定位数组多长
 *   4. 误判率（fpp, false positive probability）—— "说存在但其实不存在"的概率
 *
 * Guava 的 BloomFilter.create() 会根据 expectedInsertions 和 fpp
 * 自动计算最优的位数组长度和哈希函数个数，无需我们手算。
 */
@Configuration
public class BloomFilterConfig {

    /**
     * 期望插入的元素数量
     * 这一步对应布隆过滤器的「容量规划」概念：
     * 插入数越多，位数组需要越长，否则误判率会飙升
     */
    private static final long EXPECTED_INSERTIONS = 10000;

    /**
     * 可接受的误判率（false positive probability）
     * 这一步对应布隆过滤器的「精度控制」概念：
     * fpp 越小 → 位数组越长 → 哈希函数越多 → 判断越准，但占内存更多
     * 0.01 意味着：100 次查询中，最多允许 1 次"误判存在"
     */
    private static final double FPP = 0.01;

    /**
     * 声明布隆过滤器 Bean
     *
     * Funnels.stringFunnel(StandardCharsets.UTF_8) 告诉布隆过滤器：
     *   输入元素是 String 类型，用 UTF-8 编码转为字节后再做哈希
     *
     * 这一步对应布隆过滤器的「元素序列化」概念：
     *   任何元素在写入位数组之前，都要先转成字节流，才能被哈希函数处理
     */
    @Bean
    public BloomFilter<String> bloomFilter() {
        return BloomFilter.create(
                Funnels.stringFunnel(StandardCharsets.UTF_8),
                EXPECTED_INSERTIONS,
                FPP
        );
    }
}
