package com.example.spdemo.controller;

import com.example.spdemo.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 线程安全性教学 Demo 统一入口
 *
 * ⚠️ 注意：所有 unsafe 和 safe 接口都需要并发请求才能看到效果
 * 直接用浏览器访问单次是看不出来的（单线程执行总是正确）。
 *
 * 💥 并发触发方式（任选一种）：
 *   1. 浏览器开多个标签页同时按 Enter
 *   2. 使用 Apache Bench: ab -n 100 -c 10 "http://localhost:10000/api/primitive/unsafe?times=1000"
 *   3. 使用 curl 循环: for i in `seq 10`; do curl "http://localhost:10000/api/primitive/unsafe?times=1000" & done
 *
 * 💡 常见误区：
 *   - "用了 AtomicInteger 就一定安全" → ❌ 错！组合操作仍需同步
 *   - "局部变量也要用 Atomic" → ❌ 错！局部变量天然安全
 */
@RestController
@RequestMapping("/api")
public class ThreadSafeController {

    @Autowired
    private PrimitiveTypeService primitiveTypeService;

    @Autowired
    private ObjectReferenceService objectReferenceService;

    @Autowired
    private CollectionService collectionService;

    @Autowired
    private BeanScopeService beanScopeService;

    // ==================== 1. 基本类型 ====================

    /**
     * ❌ 完全错误：int 成员变量，并发时丢失更新
     */
    @GetMapping("/primitive/unsafe")
    public Map<String, Object> primitiveUnsafe(@RequestParam(defaultValue = "1000") int times) {
        return primitiveTypeService.unsafeIncrement(times);
    }

    /**
     * ⚠️ 看似安全实则不安全：AtomicInteger 单步是原子的，
     *    但 set(0) + N次 incrementAndGet() 整体不是原子操作。
     *    并发时 set(0) 互相覆盖，match 失败。
     */
    @GetMapping("/primitive/safe")
    public Map<String, Object> primitiveSafe(@RequestParam(defaultValue = "1000") int times) {
        return primitiveTypeService.safeIncrement(times);
    }

    /**
     * ✅ 真正安全方式：synchronized 锁住整个操作方法
     *    原理：synchronized 保证同一时刻只有一个线程执行此方法
     *    set(0) + addAndGet(times) + get() 作为一个整体，永远 match
     */
    @GetMapping("/primitive/synced")
    public Map<String, Object> primitiveSynced(@RequestParam(defaultValue = "1000") int times) {
        return primitiveTypeService.syncedIncrement(times);
    }

    /**
     * ❌ 也不安全：set(0) + addAndGet(times) + get() 整体不是原子
     *    set(0) 会被其他线程的 set(0) 覆盖，导致 match=false
     */
    @GetMapping("/primitive/atomic-add")
    public Map<String, Object> primitiveAtomicAdd(@RequestParam(defaultValue = "1000") int times) {
        return primitiveTypeService.atomicAddIncrement(times);
    }

    /**
     * ✅ 真正安全方式：局部变量（栈私有，天然线程安全）
     */
    @GetMapping("/primitive/local")
    public Map<String, Object> primitiveLocal(@RequestParam(defaultValue = "1000") int times) {
        return primitiveTypeService.localVariableIncrement(times);
    }

    // ==================== 2. 对象引用 ====================

    @GetMapping("/object/unsafe")
    public Map<String, Object> objectUnsafe(@RequestParam String name, @RequestParam int age) {
        return objectReferenceService.unsafeModifyUser(name, age);
    }

    @GetMapping("/object/safe")
    public Map<String, Object> objectSafe(@RequestParam String name, @RequestParam int age) {
        return objectReferenceService.safeLocalUser(name, age);
    }

    @GetMapping("/object/state")
    public Map<String, Object> objectState() {
        return objectReferenceService.getSharedUserState();
    }

    // ==================== 3. 集合类 ====================

    @GetMapping("/collection/list/unsafe")
    public Map<String, Object> listUnsafe(@RequestParam(defaultValue = "1000") int count) {
        return collectionService.unsafeListAdd(count);
    }

    @GetMapping("/collection/list/safe")
    public Map<String, Object> listSafe(@RequestParam(defaultValue = "1000") int count) {
        return collectionService.safeListAdd(count);
    }

    @GetMapping("/collection/map/unsafe")
    public Map<String, Object> mapUnsafe(@RequestParam(defaultValue = "1000") int count) {
        return collectionService.unsafeMapPut(count);
    }

    @GetMapping("/collection/map/safe")
    public Map<String, Object> mapSafe(@RequestParam(defaultValue = "1000") int count) {
        return collectionService.safeMapPut(count);
    }

    @GetMapping("/collection/set/unsafe")
    public Map<String, Object> setUnsafe(@RequestParam(defaultValue = "1000") int count) {
        return collectionService.unsafeSetAdd(count);
    }

    @GetMapping("/collection/set/safe")
    public Map<String, Object> setSafe(@RequestParam(defaultValue = "1000") int count) {
        return collectionService.safeSetAdd(count);
    }

    // ==================== 4. Spring Bean 作用域 ====================

    @GetMapping("/bean/unsafe")
    public Map<String, Object> beanUnsafe(@RequestParam(defaultValue = "client") String client) {
        return beanScopeService.unsafeCount(client);
    }

    @GetMapping("/bean/safe")
    public Map<String, Object> beanSafe(@RequestParam(defaultValue = "client") String client) {
        return beanScopeService.safeAtomicCount(client);
    }

    @GetMapping("/bean/stateless")
    public Map<String, Object> beanStateless(@RequestParam(defaultValue = "client") String client,
                                              @RequestParam(defaultValue = "5") int input) {
        return beanScopeService.statelessProcess(client, input);
    }

    @PostMapping("/bean/reset")
    public Map<String, Object> beanReset() {
        beanScopeService.reset();
        Map<String, Object> result = new HashMap<>();
        result.put("message", "Counters reset");
        return result;
    }
}
