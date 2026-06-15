package com.example.spdemo.service;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 场景 3 & 4：集合类的线程安全问题与线程安全替代方案
 *
 * ❌ 错误示范：ArrayList / HashMap / HashSet 在多线程下并发修改
 * ✅ 正确示范：ConcurrentHashMap / CopyOnWriteArrayList / Collections.synchronizedXxx
 *
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║ 🏷️  淘汰/标注速览                                                     ║
 * ║ ─────────────────────────────────────────────────────────────────────── ║
 * ║ ArrayList           → ❌ 禁止用于 Spring 单例 Bean（非线程安全）       ║
 * ║ HashMap             → ❌ 禁止用于 Spring 单例 Bean（非线程安全）       ║
 * ║ HashSet             → ❌ 禁止用于 Spring 单例 Bean（非线程安全）       ║
 * ║ LinkedList          → ❌ 禁止用于 Spring 单例 Bean（非线程安全）       ║
 * ║ Vector              → ⚠️ 线程安全但已过时（JDK 1.0，全方法 synchronized）║
 * ║ Hashtable           → ⚠️ 线程安全但已过时（JDK 1.0，全方法 synchronized）║
 * ║ CopyOnWriteArrayList→ ✅ 推荐，读写分离，适合读多写少场景               ║
 * ║ ConcurrentHashMap   → ✅ 推荐，分段锁/CAS+sync，高并发首选             ║
 * ║ CopyOnWriteArraySet → ✅ 推荐，基于 COW 的 Set 变体                   ║
 * ║ Collections.syncXxx → ⚠️ 不推荐，全表锁性能差，优先用 juc 集合         ║
 * ║ ArrayBlockingQueue  → ✅ 推荐，有界阻塞队列，线程安全                   ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
@Service
public class CollectionService {

    // ========== ❌ 线程不安全的集合（禁止在单例 Bean 中使用）==========
    private final List<String> unsafeList = new ArrayList<>();
    private final Map<String, Integer> unsafeMap = new HashMap<>();
    private final Set<String> unsafeSet = new HashSet<>();

    // ========== ✅ 线程安全的集合 ==========
    private final List<String> safeList = new CopyOnWriteArrayList<>();
    private final Map<String, Integer> safeMap = new ConcurrentHashMap<>();
    private final Set<String> safeSet = ConcurrentHashMap.newKeySet(); // Java 8+ 方式

    // ========== ArrayList 演示 ==========

    /**
     * 线程不安全：多线程向 ArrayList 添加元素
     * 风险：
     * 1. size++ 非原子，导致元素覆盖丢失
     * 2. 扩容时并发修改可能导致 ArrayIndexOutOfBoundsException
     */
    public Map<String, Object> unsafeListAdd(int count) {
        unsafeList.clear();
        for (int i = 0; i < count; i++) {
            unsafeList.add("item-" + i);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("expectedSize", count);
        result.put("actualSize", unsafeList.size());
        result.put("match", unsafeList.size() == count);
        return result;
    }

    /**
     * 线程安全：CopyOnWriteArrayList
     * 原理：写操作加锁并复制新数组，读操作无锁（读多写少场景性能优异）
     * 注意：内存开销大，不适合写频繁场景
     */
    public synchronized Map<String, Object> safeListAdd(int count) {
        safeList.clear();
        for (int i = 0; i < count; i++) {
            safeList.add("item-" + i);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("expectedSize", count);
        result.put("actualSize", safeList.size());
        result.put("match", safeList.size() == count);
        return result;
    }

    // ========== HashMap 演示 ==========

    /**
     * 线程不安全：多线程向 HashMap put
     * 风险：
     * 1. 链表/树节点并发操作导致死循环或数据丢失（JDK7 链表头插法还会导致死循环）
     * 2. resize 时并发 rehash 导致链表环
     */
    public Map<String, Object> unsafeMapPut(int count) {
        unsafeMap.clear();
        for (int i = 0; i < count; i++) {
            unsafeMap.put("key-" + i, i);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("expectedSize", count);
        result.put("actualSize", unsafeMap.size());
        result.put("match", unsafeMap.size() == count);
        return result;
    }

    /**
     * 线程安全：ConcurrentHashMap
     * 原理：分段锁（JDK7）或 CAS + synchronized 节点（JDK8），细粒度锁，并发度高
     */
    public synchronized Map<String, Object> safeMapPut(int count) {
        safeMap.clear();
        for (int i = 0; i < count; i++) {
            safeMap.put("key-" + i, i);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("expectedSize", count);
        result.put("actualSize", safeMap.size());
        result.put("match", safeMap.size() == count);
        return result;
    }

    // ========== HashSet 演示 ==========

    /**
     * 线程不安全：HashSet 底层就是 HashMap，风险同上
     */
    public Map<String, Object> unsafeSetAdd(int count) {
        unsafeSet.clear();
        for (int i = 0; i < count; i++) {
            unsafeSet.add("val-" + i);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("expectedSize", count);
        result.put("actualSize", unsafeSet.size());
        result.put("match", unsafeSet.size() == count);
        return result;
    }

    /**
     * 线程安全：使用 ConcurrentHashMap.newKeySet()
     */
    public synchronized Map<String, Object> safeSetAdd(int count) {
        safeSet.clear();
        for (int i = 0; i < count; i++) {
            safeSet.add("val-" + i);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("expectedSize", count);
        result.put("actualSize", safeSet.size());
        result.put("match", safeSet.size() == count);
        return result;
    }
}
