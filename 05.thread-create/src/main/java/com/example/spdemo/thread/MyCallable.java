package com.example.spdemo.thread;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Callable;

/**
 * ============================================================
 * 方式三：实现 Callable 接口 + FutureTask 创建线程
 * ============================================================
 * 
 * 【核心思路】
 *   方式一（继承 Thread）和方式二（实现 Runnable）都有一个共同的限制：
 *   run() 方法没有返回值。很多时候我们想让线程执行完任务后返回一个结果，
 *   这时候就需要 Callable 了。
 * 
 *   Callable<V> 是一个泛型接口，V 就是返回值的类型。它只有一个方法：
 *     V call() throws Exception;
 *   和 Runnable 的 run() 类似，但 call() 有返回值、可以抛异常。
 * 
 *   但是！Thread 的构造函数只接受 Runnable，不接受 Callable。
 *   所以需要一个"桥梁"——FutureTask。
 *   FutureTask 实现了 RunnableFuture（Runnable 的子接口），
 *   所以它可以被 Thread 接受。同时 FutureTask 内部会持有 Callable，
 *   当线程运行时，FutureTask 会调用 Callable 的 call() 方法，
 *   并把返回值存起来供后续获取。
 * 
 * 【使用步骤】
 *   1. 创建 Callable 实现类，实现 call() 方法
 *   2. 创建 FutureTask 对象，把 Callable 传进去
 *   3. new Thread(futureTask).start() 启动线程
 *   4. 用 futureTask.get() 获取返回值（这个方法会阻塞，直到线程执行完）
 * 
 * 【注意】
 *   futureTask.get() 会阻塞当前线程，直到异步任务执行完毕。
 *   如果不想无限等待，可以用 get(long timeout, TimeUnit unit) 设置超时。
 *   如果任务还没执行完就调 get()，调用线程会进入 WAITING 状态。
 * 
 * 【优点】
 *   - 有返回值，可以拿到线程执行的结果
 *   - 可以抛出受检异常，调用者可以处理
 * 
 * 【缺点】
 *   - 每次任务仍然要 new Thread()，高并发场景下线程数不可控
 *   - get() 会阻塞，如果任务耗时久，调用线程会一直等着
 * 
 * 【面试常见问题】
 *   Q: Callable 和 Runnable 的区别？
 *   A: 1) Callable 有返回值，Runnable 没有
 *      2) Callable 的 call() 能抛异常，Runnable 的 run() 不能
 *      3) Callable 用泛型指定返回值类型
 * 
 *   Q: FutureTask 是什么？
 *   A: FutureTask 是 Runnable 和 Future 的实现类，
 *      它既可以当 Runnable 传给 Thread，又可以当 Future 获取结果。
 *      底层的本质是：它包装了一个 Callable，由线程执行完毕后把结果存起来。
 * 
 * 【适合场景】
 *   需要异步执行并获取返回结果的场景（但线程数可控时不如线程池方便）
 */
public class MyCallable implements Callable<String> {

    /**
     * 实现 Callable 接口的 call() 方法
     * 这个方法由 FutureTask 内部的机制调用，执行完后返回值会被 FutureTask 保存
     * 
     * @return 线程执行的结果（字符串）
     * @throws Exception 可以抛任何异常
     */
    @Override
    public String call() throws Exception {
        // Thread.currentThread() 获取当前正在执行的线程对象
        String threadName = Thread.currentThread().getName();

        // 获取当前时间，格式化为 "HH:mm:ss.SSS"
        String now = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));

        // 组装要打印的信息
        String msg = String.format(
                "[%s] %s —— 方式三：实现 Callable + FutureTask",
                threadName,
                now
        );

        // 打印到控制台
        System.out.println(msg);

        // 返回结果，调用方可以通过 futureTask.get() 拿到这个字符串
        return msg;
    }
}