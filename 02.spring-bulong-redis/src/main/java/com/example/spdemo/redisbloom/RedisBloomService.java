package com.example.spdemo.redisbloom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * 布隆过滤器服务
 *
 * 直接使用 RedisTemplate.execute() + RedisCallback 执行原生 RedisBloom 命令，
 * 不依赖任何第三方 RedisBloom Java 客户端。
 *
 * 命令对照：
 *   BF.RESERVE key error_rate capacity  → 创建/初始化布隆过滤器
 *   BF.ADD key item                     → 向布隆过滤器添加元素
 *   BF.EXISTS key item                  → 检查元素是否可能存在
 */
@Service
public class RedisBloomService {

    private static final Logger log = LoggerFactory.getLogger(RedisBloomService.class);

    @Autowired
    private StringRedisTemplate redisTemplate;

    // ==================== 初始化 ====================

    /**
     * 初始化布隆过滤器，并预填演示数据
     *
     * 教学说明：
     * BF.RESERVE 的语义：
     *   1. 参数1（key）: 布隆过滤器的名称，对应 Redis 中的一个 key
     *   2. 参数2（error_rate）: 误判率，取值范围 (0, 1)
     *   3. 参数3（capacity）: 预期存储的元素个数
     *
     * 底层发生了什么：
     *   RedisBloom 会根据 error_rate 和 capacity 自动计算：
     *   - 位数组（bit array）需要多大
     *   - 需要多少个哈希函数
     *   然后分配一块内存作为位数组。
     *
     * 注意：BF.RESERVE 只能对不存在的 key 调用，
     *       如果 key 已存在会报 "ERR item exists" 错误，
     *       所以我们先尝试删除再创建。
     */
    public void initBloomFilter() {
        redisTemplate.execute((RedisCallback<Void>) connection -> {
            // 先删除旧过滤器（如果 key 不存在，DEL 也不会报错）
            connection.del(raw(BLOOM_KEY));

            // BF.RESERVE key error_rate capacity
            // 创建布隆过滤器：容量10000，误判率0.01（即1%）
            connection.execute("BF.RESERVE",
                    raw(BLOOM_KEY),
                    raw(String.valueOf(FPP)),
                    raw(String.valueOf(CAPACITY)));

            log.info("[初始化] BF.RESERVE 成功: key={}, error_rate={}, capacity={}",
                    BLOOM_KEY, FPP, CAPACITY);
            return null;
        });

        // 预填 5 条演示数据（模拟"这些用户ID在数据库中是真实存在的"）
        List<String> demoIds = Arrays.asList("1001", "1002", "1003", "1004", "1005");
        for (String id : demoIds) {
            add(id);
        }
        log.info("[初始化] 已预填 {} 条演示数据: {}", demoIds.size(), demoIds);
    }

    // ==================== 添加元素 ====================

    /**
     * 向布隆过滤器添加元素
     *
     * 教学说明：
     * BF.ADD 的工作流程：
     *   1. 将元素经过 k 个不同的哈希函数，得到 k 个哈希值
     *   2. 每个哈希值对位数组长度取模，得到 k 个位置
     *   3. 将这 k 个位置（bit）都设为 1
     *
     * 返回值：
     *   - 1（本次调用成功将至少 1 位从 0 翻转为 1）→ 元素之前一定不在过滤器中
     *   - 0（所有 k 位原本就已经是 1）        → 元素可能已经存在
     *
     * 注意：由于哈希碰撞，"返回 1"的极小概率下也可能存在已添加的元素。
     *       不过这一点对于布隆过滤器的使用场景通常无关紧要。
     *
     * @param value 要添加的元素
     * @return true 表示之前不存在（新添加），false 表示可能已存在
     */
    public boolean add(String value) {
        Boolean result = redisTemplate.execute((RedisCallback<Boolean>) connection -> {
            // BF.ADD key item
            Long reply = (Long) connection.execute("BF.ADD",
                    raw(BLOOM_KEY),
                    raw(value));
            return reply != null && reply == 1L;
        });

        boolean isNew = Boolean.TRUE.equals(result);
        log.info("[添加] BF.ADD key={}, value={}, isNewlyAdded={}", BLOOM_KEY, value, isNew);
        return isNew;
    }

    // ==================== 查询元素 ====================

    /**
     * 判断元素是否"可能存在于"布隆过滤器中
     *
     * 教学说明（★ 核心教学点）：
     * BF.EXISTS 的执行过程：
     *   1. 和 BF.ADD 一样，计算元素的 k 个哈希位置
     *   2. 检查这 k 个位置是否全为 1
     *
     * 返回值语义：
     *   - 返回 1（true）→ "可能存在"
     *       说明：所有 k 个位置都是 1，但可能是：
     *         a) 之前确实添加过这个元素（True Positive）
     *         b) 其他元素碰巧把这 k 个位置都置为 1 了（False Positive / 误判）
     *       在缓存穿透场景中，这种情况继续走缓存→数据库的常规查询流程。
     *
     *   - 返回 0（false）→ "一定不存在" ← 这是布隆过滤器的核心价值！
     *       说明：至少有一个位置是 0，说明这个元素从未被添加过。
     *       这是 100% 确定的结论，绝对不会出错。
     *       在缓存穿透场景中，直接返回空，不会再查询数据库。
     *
     * @param value 要检查的元素
     * @return true=可能存在, false=一定不存在
     */
    public boolean mightContain(String value) {
        Boolean result = redisTemplate.execute((RedisCallback<Boolean>) connection -> {
            // BF.EXISTS key item
            Long reply = (Long) connection.execute("BF.EXISTS",
                    raw(BLOOM_KEY),
                    raw(value));
            return reply != null && reply == 1L;
        });

        boolean mayExist = Boolean.TRUE.equals(result);
        log.info("[查询] BF.EXISTS key={}, value={}, mayExist={} → {}",
                BLOOM_KEY, value, mayExist,
                mayExist ? "可能存在，需要进一步查缓存/数据库" : "一定不存在，直接拦截");
        return mayExist;
    }

    // ==================== 辅助方法 ====================

    /**
     * 将字符串转为 UTF-8 字节数组，用于通过 Connection 发送原始 Redis 命令
     */
    private byte[] raw(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    // ==================== 从配置常量导入 ====================

    private static final String BLOOM_KEY = RedisBloomConfig.BLOOM_FILTER_KEY;
    private static final double FPP = RedisBloomConfig.FALSE_POSITIVE_PROBABILITY;
    private static final long CAPACITY = RedisBloomConfig.EXPECTED_INSERTIONS;
}
