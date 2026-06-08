package com.example.spdemo.redisbloom;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 布隆过滤器 REST 接口
 *
 * 用于演示和调试布隆过滤器的添加/查询操作。
 *
 * 接口列表：
 *   GET /redis-bloom/add?value=xxx    → 向布隆过滤器添加一个值
 *   GET /redis-bloom/contains?value=xxx → 查询一个值是否可能存在
 */
@RestController
@RequestMapping("/redis-bloom")
public class RedisBloomController {

    @Autowired
    private RedisBloomService bloomService;

    /**
     * 添加元素到布隆过滤器
     *
     * 示例：curl "http://localhost:10000/redis-bloom/add?value=12345"
     *
     * 返回字段说明：
     *   - isNew: true 表示元素之前不存在，已成功添加
     *   - bloomFilter: 标记该操作来自布隆过滤器
     */
    @GetMapping("/add")
    public Map<String, Object> add(@RequestParam String value) {
        boolean isNew = bloomService.add(value);
        Map<String, Object> result = new HashMap<>();
        result.put("action", "add");
        result.put("value", value);
        result.put("isNew", isNew);
        result.put("bloomFilter", RedisBloomConfig.BLOOM_FILTER_KEY);
        result.put("note", isNew
                ? "元素已成功添加到布隆过滤器"
                : "元素可能已存在（所有哈希位已是1）");
        return result;
    }

    /**
     * 检查元素是否可能存在于布隆过滤器中
     *
     * 示例：curl "http://localhost:10000/redis-bloom/contains?value=1001"
     *
     * 返回字段说明：
     *   - mayExist: true=可能存在, false=一定不存在
     *   - actionIfMayExist: 建议的后续动作
     */
    @GetMapping("/contains")
    public Map<String, Object> contains(@RequestParam String value) {
        boolean mayExist = bloomService.mightContain(value);
        Map<String, Object> result = new HashMap<>();
        result.put("action", "contains");
        result.put("value", value);
        result.put("mayExist", mayExist);
        result.put("bloomFilter", RedisBloomConfig.BLOOM_FILTER_KEY);

        if (mayExist) {
            // 可能存在 → 需要进一步查缓存或数据库
            result.put("verdict", "可能存在");
            result.put("suggestion", "继续查询缓存/数据库以确认");
        } else {
            // 一定不存在 → 可以直接拦截，防止缓存穿透
            result.put("verdict", "一定不存在");
            result.put("suggestion", "直接返回空结果，无需查询数据库（缓存穿透拦截成功!）");
        }
        return result;
    }
}
