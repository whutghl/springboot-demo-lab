package com.example.spdemo.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 场景 1：基本类型 (int / long) 的线程安全性
 *
 * ❌ 错误示范：使用普通成员变量在多线程累加时会出现丢失更新
 * ✅ 正确示范：使用 AtomicInteger / AtomicLong 保证原子性
 *
 * ╔══════════════════════════════════════════════════════════════╗
 * ║ 🏷️  淘汰/标注速览                                         ║
 * ║ ─────────────────────────────────────────────────────────── ║
 * ║ int / long 成员变量    → ❌ 禁止用于 Spring 单例 Bean      ║
 * ║                          (可变状态，所有请求线程共享)        ║
 * ║ AtomicInteger          → ✅ 推荐，CAS 保证原子性             ║
 * ║ AtomicLong             → ✅ 推荐，CAS 保证原子性             ║
 * ║ LongAdder (JDK8+)     → ✅ 更优选择（高并发场景推荐）        ║
 * ╚══════════════════════════════════════════════════════════════╝
 */
@Service
public class PrimitiveTypeService {

    // ========== ❌ 线程不安全的成员变量（禁止在单例 Bean 中使用）==========
    private int unsafeCounter = 0;

    // ========== ⚠️ 原子类单步操作安全，但组合操作仍需要外部同步 ==========
    private final AtomicInteger safeCounter = new AtomicInteger(0);

    /**
     * ❌ 线程不安全：int 成员变量在多线程下累加
     * 问题：i++ = read + modify + write 三步非原子，并发时丢失更新
     * 复现方式：并发请求此 API，多次调用即可看到 actual ≠ expected
     */
    public Map<String, Object> unsafeIncrement(int times) {
        unsafeCounter = 0;
        for (int i = 0; i < times; i++) {
            unsafeCounter++; // 非原子操作
        }
        Map<String, Object> result = new HashMap<>();
        result.put("expected", times);
        result.put("actual", unsafeCounter);
        result.put("match", unsafeCounter == times);
        return result;
    }

    /**
     * ⚠️ 看起来"安全"但其实也有问题：AtomicInteger 单次操作是原子的，
     *     但 set(0) + N次 incrementAndGet() 整个序列不是原子的。
     *     并发请求时 set(0) 互相覆盖，导致最终值 ≠ times × 请求数。
     * 
     * 教训：即使用了原子类，组合操作（check-then-act）仍需要外部同步。
     */
    public Map<String, Object> safeIncrement(int times) {
        safeCounter.set(0);
        for (int i = 0; i < times; i++) {
            safeCounter.incrementAndGet();
        }
        Map<String, Object> result = new HashMap<>();
        result.put("expected", times);
        result.put("actual", safeCounter.get());
        result.put("match", safeCounter.get() == times);
        return result;
    }

    /**
     * ❌ 看似安全实则也不安全：addAndGet(times) 是原子的，但
     *    set(0) + addAndGet(times) + get() 这个序列整体不是原子的。
     *    set(0) 和 get() 之间可能被其他线程的 set(0) 干扰。
     *
     * 教训：原子类的每个单步操作是原子的，但组合操作不是。
     *      只有 synchronized / Lock 才能保证整段代码的原子性。
     */
    public Map<String, Object> atomicAddIncrement(int times) {
        safeCounter.set(0);
        try {
            Thread.sleep(5);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    
        safeCounter.addAndGet(times);
        Map<String, Object> result = new HashMap<>();
        result.put("expected", times);
        result.put("actual", safeCounter.get());
        result.put("match", safeCounter.get() == times);
        return result;
    }

    /**
     * ✅ 真正安全的做法：synchronized 锁住整个操作方法
     * 原理：synchronized 保证同一时刻只有一个线程执行此方法
     *       set(0) + addAndGet(times) + get() 作为一个整体执行
     *       这才是唯一真正安全的方案（当需要共享成员变量时）
     */
    public synchronized Map<String, Object> syncedIncrement(int times) {
        safeCounter.set(0);
        safeCounter.addAndGet(times);
        Map<String, Object> result = new HashMap<>();
        result.put("expected", times);
        result.put("actual", safeCounter.get());
        result.put("match", safeCounter.get() == times);
        return result;
    }

    /**
     * ✅ 真正安全的做法：局部变量（栈私有，天然无竞争）
     * 原理：方法内局部变量存在线程栈中，其他线程完全不可见
     * 适用场景：无需共享状态时，这是最简单高效的方式
     */
    public Map<String, Object> localVariableIncrement(int times) {
        int localCounter = 0;
        for (int i = 0; i < times; i++) {
            localCounter++;
        }
        Map<String, Object> result = new HashMap<>();
        result.put("expected", times);
        result.put("actual", localCounter);
        result.put("match", localCounter == times);
        return result;
    }
}
