package com.example.spdemo.service;

import com.example.spdemo.entity.User;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 场景 2：对象引用与 POJO 成员变量的线程安全性
 *
 * ❌ 错误示范：共享可变对象引用，多线程修改会导致数据混乱
 * ✅ 正确示范：方法内使用局部变量（栈私有）或返回不可变对象
 *
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║ 🏷️  淘汰/标注速览                                             ║
 * ║ ─────────────────────────────────────────────────────────────── ║
 * ║ 共享的可变 POJO 成员变量 → ❌禁止用于 Spring 单例 Bean         ║
 * ║                            (所有请求共享同一对象引用，竞态重写)  ║
 * ║ 方法内局部 new 出的 POJO → ✅ 线程安全（栈私有，天然隔离）     ║
 * ║ @Data 注解的 POJO        → ⚠️ 可序列化但非线程安全             ║
 * ║                            (setter 无锁，多线程并发不安全)       ║
 * ╚══════════════════════════════════════════════════════════════════╝
 */
@Service
public class ObjectReferenceService {

    // ========== ❌ 共享的可变成员变量（禁止在单例 Bean 中使用）==========
    private User sharedUser = new User("Alice", 20);

    /**
     * 线程不安全：直接修改共享的 POJO 成员变量
     * 风险：多线程同时 setName/setAge，最终结果不可预期
     */
    public Map<String, Object> unsafeModifyUser(String name, int age) {
        sharedUser.setName(name);
        sharedUser.setAge(age);

        // 模拟一点点处理延迟，放大竞态条件
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("currentName", sharedUser.getName());
        result.put("currentAge", sharedUser.getAge());
        result.put("thread", Thread.currentThread().getName());
        return result;
    }

    /**
     * 线程安全：使用局部变量，每个请求线程操作自己的对象
     * 原理：局部变量存在于线程私有栈帧，天然隔离
     */
    public Map<String, Object> safeLocalUser(String name, int age) {
        User localUser = new User(name, age); // 每次新建对象

        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("currentName", localUser.getName());
        result.put("currentAge", localUser.getAge());
        result.put("thread", Thread.currentThread().getName());
        return result;
    }

    /**
     * 获取当前共享对象状态（用于观察竞态后果）
     */
    public Map<String, Object> getSharedUserState() {
        Map<String, Object> result = new HashMap<>();
        result.put("name", sharedUser.getName());
        result.put("age", sharedUser.getAge());
        return result;
    }
}
