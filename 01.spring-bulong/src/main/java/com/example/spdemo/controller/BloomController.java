package com.example.spdemo.controller;

import com.example.spdemo.service.BloomFilterService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 布隆过滤器演示接口
 *
 * 提供两个 GET 端点，方便用 curl 快速验证布隆过滤器的行为：
 *   /bloom/add?value=xxx      → 添加元素
 *   /bloom/contains?value=xxx → 查询元素是否可能存在
 *
 * 在实际防缓存穿透的场景中：
 *   - 查询请求先过布隆过滤器（相当于本接口的 /contains）
 *   - 返回 false → 说明数据一定不在数据库，直接返回空，避免穿透到 DB
 *   - 返回 true  → 数据可能在，再去查缓存/数据库
 */
@RestController
@RequestMapping("/bloom")
public class BloomController {

    private final BloomFilterService bloomFilterService;

    public BloomController(BloomFilterService bloomFilterService) {
        this.bloomFilterService = bloomFilterService;
    }

    /**
     * 添加元素到布隆过滤器
     * 对应布隆过滤器的「写入」操作
     */
    @GetMapping("/add")
    public Map<String, Object> add(@RequestParam String value) {
        bloomFilterService.add(value);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "已添加: " + value);
        result.put("hint", "元素经多次哈希后，对应位数组位置被置为1");
        return result;
    }

    /**
     * 查询元素是否可能存在于布隆过滤器
     * 对应布隆过滤器的「查询」操作
     */
    @GetMapping("/contains")
    public Map<String, Object> contains(@RequestParam String value) {
        boolean mightExist = bloomFilterService.mightContain(value);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("value", value);
        result.put("mightExist", mightExist);
        if (mightExist) {
            result.put("hint", "布隆过滤器返回true：元素可能存在（有小概率是误判）");
        } else {
            result.put("hint", "布隆过滤器返回false：元素一定不存在（绝不会漏判）");
        }
        return result;
    }
}
