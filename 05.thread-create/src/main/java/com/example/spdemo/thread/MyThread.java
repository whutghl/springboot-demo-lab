package com.example.spdemo.thread;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * ============================================================
 * 方式一：继承 Thread 类创建线程
 * ============================================================
 * 
 * 【核心思路】
 *   写一个类，让它 extends Thread，然后重写父类的 run() 方法，
 *   把要执行的任务代码放在 run() 里面。
 *   调用时 new 出对象，调 start()，JVM 就会自动去调用 run()。
 * 
 * 【注意】
 *   千万不要直接调 run()！那样不会启动新线程，只是普通方法调用。
 *   必须调 start() 才是真正启动一个新线程。
 * 
 * 【优点】
 *   - 写法最简单，适合快速演示
 *   - 子类可以直接使用 Thread 类的各种方法（getName、setPriority 等）
 * 
 * 【缺点】
 *   - Java 是单继承，一个类继承了 Thread 就不能再继承别的类了，扩展性很差
 *   - 任务逻辑和线程代码写在一起，职责不分离（违反了"单一职责原则"）
 *   - 不能有返回值，不能抛受检异常
 * 
 * 【面试常见问题】
 *   Q: start() 和 run() 的区别？
 *   A: start() 会创建新线程并由新线程执行 run()；run() 只是在当前线程里执行普通方法。
 * 
 * 【适合场景】
 *   仅仅适合入门学习演示，实际项目很少用。
 */
public class MyThread extends Thread {

    /**
     * 重写父类 Thread 的 run() 方法
     * 当调用 start() 后，新线程会执行这里的代码
     */
    @Override
    public void run() {
        // Thread.currentThread() 获取当前正在执行的线程对象
        // getName() 获取线程的名字（默认是 Thread-0, Thread-1 ...）
        String threadName = Thread.currentThread().getName();

        // 获取当前的日期时间，并格式化为 "HH:mm:ss.SSS"（时:分:秒.毫秒）
        String now = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));

        // 组装要打印的信息
        String msg = String.format(
                "[%s] %s —— 方式一：继承 Thread 类",
                threadName,
                now
        );

        // 打印到控制台，面试时通常用 System.out 做简单演示
        System.out.println(msg);
    }
}