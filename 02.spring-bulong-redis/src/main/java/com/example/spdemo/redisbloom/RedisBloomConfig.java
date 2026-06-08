package com.example.spdemo.redisbloom;

/**
 * Redis Bloom Filter 配置常量
 *
 * ======================== 教学说明 ========================
 *
 * 【布隆过滤器是什么？】
 * 布隆过滤器（Bloom Filter）是一个概率性数据结构，用于判断
 * "一个元素是否可能存在于集合中"。
 *
 * 它能给出的答案只有两种：
 *   1. "可能存在" —— 元素也许在集合里（有误判概率）
 *   2. "一定不存在" —— 元素绝对不在集合里（100% 准确）
 *
 * 【为什么能解决缓存穿透？】
 * 缓存穿透：攻击者用大量不存在的ID疯狂请求，每次都绕开缓存直击数据库。
 *
 * 布隆过滤器解决方案：
 *   Step 1 - 预热阶段：系统启动时，把所有数据库已有ID写入布隆过滤器
 *   Step 2 - 查询阶段：请求来了，先问布隆过滤器
 *     → "一定不存在" → 直接返回空，不查缓存，不查数据库 ✓
 *     → "可能存在"   → 正常走 缓存→数据库 流程
 *
 * 【核心参数说明】
 * - capacity（预期容量）：你预计要存多少个元素
 * - FPP（误判率）：你愿意接受多大的误判概率
 * - 这两个参数共同决定了位数组的大小和哈希函数数量
 *
 * 【RedisBloom vs 手写】
 * RedisBloom（Redis Stack 模块）使用原生 C 实现的布隆过滤器：
 * - 命令：BF.RESERVE / BF.ADD / BF.EXISTS（以及更多高级命令）
 * - 底层使用可扩展布隆过滤器（Scalable Bloom Filter），自动扩容
 * - 不占用 Redis 主线程，性能优异
 * - 无需手敲 SETBIT/GETBIT
 *
 * ===========================================================
 */
public class RedisBloomConfig {

    /**
     * 布隆过滤器在 Redis 中的 Key 名称
     *
     * 教学说明：
     * 布隆过滤器在 Redis 中也是通过一个 key 来索引的，
     * 这个 key 指向的就是一个位数组（bit array）数据结构。
     * Key 命名规范：业务前缀 + 实体类型，如 "bloom:user:ids"
     */
    public static final String BLOOM_FILTER_KEY = "bloom:user:ids";

    /**
     * 预计插入元素数量（capacity）
     *
     * 教学说明：
     * - 这个值应该 ≥ 你的实际数据量
     * - 如果实际数据量超过预期容量，误判率会急剧上升
     * - 布隆过滤器的内存占用 = f(capacity, FPP)
     *   公式：m（位数组bit数）≈ -(n * ln(p)) / (ln(2)^2)
     *   其中 n=capacity，p=FPP
     *   本例：m ≈ -(10000 * ln(0.01)) / 0.48 ≈ 95850 bits ≈ 12KB
     */
    public static final long EXPECTED_INSERTIONS = 10000L;

    /**
     * 误判率（False Positive Probability, FPP）
     *
     * 教学说明：
     * - 0.01 = 1% 误判率
     * - 含义：100个"不存在"的查询中，约有1个会被误判为"可能存在"
     * - 误判率越低 → 需要内存越多 → 哈希函数越多 → 性能越慢
     * - 常见取值：0.01(1%)、0.001(0.1%)、0.0001(0.01%)
     *
     * 注意：误判率 ≠ 漏判率
     * - 误判（False Positive）：不存在 → 误判为存在（布隆过滤器固有缺陷）
     * - 漏判（False Negative）：存在   → 漏判为不存在（布隆过滤器不会犯这个错！）
     */
    public static final double FALSE_POSITIVE_PROBABILITY = 0.01;
}
