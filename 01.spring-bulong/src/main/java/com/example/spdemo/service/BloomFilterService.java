package com.example.spdemo.service;

import com.google.common.hash.BloomFilter;
import org.springframework.stereotype.Service;

/**
 * 布隆过滤器服务
 *
 * 只提供两个核心操作，对应布隆过滤器的全部语义：
 *   add           → 写入：把元素的多个哈希位置设为 1
 *   mightContain  → 查询：检查那些位置是否全为 1
 *
 * 注意方法名叫「mightContain」而不是「contains」，
 * 这正是布隆过滤器的关键特性：
 *   - 返回 true  → 元素**可能**存在（有误判概率）
 *   - 返回 false → 元素**一定**不存在（绝不会漏判）
 */
@Service
public class BloomFilterService {

    private final BloomFilter<String> bloomFilter;

    public BloomFilterService(BloomFilter<String> bloomFilter) {
        this.bloomFilter = bloomFilter;
    }

    /**
     * 往布隆过滤器中添加元素
     *
     * 这一步对应布隆过滤器的「写入」概念：
     *   1. 对 value 做 k 次哈希，得到 k 个位置
     *   2. 把位数组中这 k 个位置全部置为 1
     *
     * 一旦置 1 就不会再改回 0（布隆过滤器不支持删除），
     * 这也是它和 Set 的核心区别之一。
     */
    public void add(String value) {
        bloomFilter.put(value);
    }

    /**
     * 判断元素是否**可能**存在于布隆过滤器中
     *
     * 这一步对应布隆过滤器的「查询」概念：
     *   1. 对 value 做 k 次哈希，得到 k 个位置
     *   2. 检查位数组中这 k 个位置是否全为 1
     *      - 全为 1 → 返回 true（可能存在，也可能是别的元素碰巧把这些位置置 1 了）
     *      - 有 0   → 返回 false（一定不存在，因为写入时会把所有位置都置 1）
     *
     * 正是因为"全为1也可能是别人碰巧设的"，所以叫 mightContain 而非 contains。
     * 这就是布隆过滤器能挡缓存穿透的核心原理：
     *   对数据库里绝对不存在的 key，mightContain 返回 false → 直接拦截，不查数据库。
     */
    public boolean mightContain(String value) {
        return bloomFilter.mightContain(value);
    }
}
