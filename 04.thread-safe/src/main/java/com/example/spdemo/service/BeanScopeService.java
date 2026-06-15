package com.example.spdemo.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 场景 5：Spring Bean 默认作用域 (Singleton) 的线程安全问题
 *
 * ❌ 错误示范：Singleton Bean 中存在可变状态（成员变量）
 * ✅ 正确示范：
 *    1. Bean 无状态化（所有数据通过参数传递、局部变量处理）
 *    2. 或者使用 @Scope("prototype") / @Scope("request")（视场景而定）
 *
 * ╔══════════════════════════════════════════════════════════════════════╗
 * ║ 🏷️  淘汰/标注速览                                                 ║
 * ║ ─────────────────────────────────────────────────────────────────── ║
 * ║ Singleton Bean 中可变成员变量 → ❌不能用于多线程环境               ║
 * ║                                  (同一实例被所有请求共享)            ║
 * ║ @Scope("prototype")          → ✅ 每个请求新建实例，安全但浪费      ║
 * ║ @Scope("request")            → ✅ 每个 HTTP 请求一个实例            ║
 * ║ @Scope("session")            → ✅ 每个用户会话一个实例              ║
 * ║ ThreadLocal                  → ✅ 线程级隔离，但注意内存泄漏        ║
 * ║ @Async 方法中的成员变量      → ❌ 异步线程共享，非线程安全          ║
 * ╚══════════════════════════════════════════════════════════════════════╝
 */
@Service
public class BeanScopeService {

    // ========== ❌ Singleton Bean 中的可变状态（禁止！）==========
    // Spring 默认单例，整个应用只有一个 BeanScopeService 实例
    // 这个成员变量被所有请求线程共享
    private int visitCount = 0;

    // ========== ✅ 使用原子类仍然可以保持在单例中安全 ==========
    private final AtomicInteger atomicVisitCount = new AtomicInteger(0);

    /**
     * 线程不安全：直接操作普通成员变量
     * 风险：多个 HTTP 请求线程并发访问，visitCount++ 非原子
     */
    public Map<String, Object> unsafeCount(String client) {
        visitCount++; // 竞态条件

        Map<String, Object> result = new HashMap<>();
        result.put("client", client);
        result.put("currentCount", visitCount);
        result.put("thread", Thread.currentThread().getName());
        return result;
    }

    /**
     * 线程安全：使用 AtomicInteger
     * 说明：虽然仍是单例共享状态，但原子类保证了操作安全
     */
    public Map<String, Object> safeAtomicCount(String client) {
        int current = atomicVisitCount.incrementAndGet();

        Map<String, Object> result = new HashMap<>();
        result.put("client", client);
        result.put("currentCount", current);
        result.put("thread", Thread.currentThread().getName());
        return result;
    }

    /**
     * 最佳实践：Service 层无状态化
     * 所有数据来自方法参数，中间结果存于局部变量
     * 这是 Spring 官方推荐做法
     */
    public Map<String, Object> statelessProcess(String client, int input) {
        // localResult 是局部变量，存在于当前线程栈，绝对安全
        int localResult = input * 2 + 1;

        Map<String, Object> result = new HashMap<>();
        result.put("client", client);
        result.put("input", input);
        result.put("output", localResult);
        result.put("thread", Thread.currentThread().getName());
        return result;
    }

    /**
     * 重置计数器（用于测试前清零）
     */
    public void reset() {
        visitCount = 0;
        atomicVisitCount.set(0);
    }
}
