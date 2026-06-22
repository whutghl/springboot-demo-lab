package com.example.spdemo.thread;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * ============================================================
 * 方式四：ThreadPoolExecutor（线程池）创建线程
 * ============================================================
 * 
 * 【核心思路】
 *   前面三种方式（继承 Thread、实现 Runnable、实现 Callable）都有一个共同问题：
 *   每次执行任务都要 new Thread()，高并发下会创建大量线程，导致系统资源耗尽（OOM）。
 * 
 *   线程池的核心思想是"复用"——提前创建好一批线程，有任务来了直接复用已有的线程，
 *   任务执行完了线程也不销毁，继续等待下一个任务。这样就避免了频繁创建/销毁线程的开销。
 * 
 * 【阿里巴巴 Java 开发手册强制要求】
 *   线程池不允许使用 Executors 去创建，而是通过 ThreadPoolExecutor 的方式，
 *   这样的处理方式让开发同学更加明确线程池的运行规则，规避资源耗尽的风险。
 *   说明：Executors.newFixedThreadPool() 默认使用 Integer.MAX_VALUE 的无界队列，
 *   高并发下大量任务积压会导致 OOM。
 * 
 * 【构造参数含义（按顺序）】
 *   1. corePoolSize    = 2  —— 核心线程数（即使空闲也保留的线程数）
 *   2. maximumPoolSize = 4  —— 最大线程数（线程数上限）
 *   3. keepAliveTime   = 60 —— 空闲线程的存活时间（单位：秒）
 *      超出 corePoolSize 的线程，空闲超过这个时间就会被回收
 *   4. unit            = TimeUnit.SECONDS —— keepAliveTime 的时间单位
 *   5. workQueue       = new LinkedBlockingQueue<>(10) —— 有界工作队列
 *      当核心线程都在忙时，新任务先放进队列排队
 *      使用有界队列（容量10），防止任务积压导致内存溢出
 *   6. threadFactory   = （没传，使用默认的）—— 线程工厂，可以自定义线程名
 *   7. handler         = new ThreadPoolExecutor.AbortPolicy() —— 拒绝策略
 *      当队列已满且线程数达到 maximumPoolSize 时，新任务会被拒绝
 *      AbortPolicy：直接抛 RejectedExecutionException（默认策略）
 *      其他可选策略：
 *        - CallerRunsPolicy：让提交任务的线程自己执行
 *        - DiscardPolicy：悄悄丢弃任务
 *        - DiscardOldestPolicy：丢弃队列中最旧的任务
 * 
 * 【线程池工作流程（非常重要，面试必问）】
 *   1. 提交一个任务
 *   2. 如果当前线程数 < corePoolSize → 创建新线程执行任务
 *   3. 如果当前线程数 >= corePoolSize → 尝试放入队列
 *   4. 如果队列已满 & 线程数 < maximumPoolSize → 创建临时线程执行
 *   5. 如果队列已满 & 线程数 >= maximumPoolSize → 执行拒绝策略
 * 
 * 【优点】
 *   - 线程复用，避免频繁创建/销毁的开销
 *   - 通过核心线程数、最大线程数、有界队列控制并发量，防止 OOM
 *   - 任务提交与执行策略解耦，代码更清晰
 *   - 支持优雅关闭（shutdown）、任务超时、拒绝策略等高级功能
 * 
 * 【适合场景】
 *   任何需要异步执行任务的项目，尤其是高并发场景，几乎都用线程池。
 */
public class MyThreadPoolExecutor {

    /**
     * 单例的线程池对象
     * 
     * 使用 static final 保证整个应用只有一个线程池实例，
     * 所有任务都通过这个线程池来执行。
     * 
     * 构造参数含义（按顺序）：
     *   corePoolSize      = 2                          —— 核心线程数
     *   maximumPoolSize   = 4                          —— 最大线程数
     *   keepAliveTime     = 60L                        —— 空闲存活时间
     *   unit              = TimeUnit.SECONDS           —— 时间单位
     *   workQueue         = new LinkedBlockingQueue<>(10) —— 有界队列，容量10
     *   handler           = new ThreadPoolExecutor.AbortPolicy() —— 拒绝策略
     */
    public static final ThreadPoolExecutor EXECUTOR = new ThreadPoolExecutor(
            2,                              // corePoolSize：核心线程数
            4,                              // maximumPoolSize：最大线程数
            60L,                            // keepAliveTime：空闲存活时间
            TimeUnit.SECONDS,               // 时间单位
            new LinkedBlockingQueue<>(10),  // 有界工作队列，容量为 10
            new ThreadPoolExecutor.AbortPolicy() // 拒绝策略：队列满+线程满时抛异常
    );
      // __选择建议：__
// - AbortPolicy（默认）：关键业务使用，宁可失败也要感知异常
// - CallerRunsPolicy：削峰填谷，让提交线程帮忙处理任务
// - DiscardPolicy：非核心可丢弃任务，如日志、统计数据
// - DiscardOldestPolicy：追求最新数据，丢弃旧任务优先处理新任务



    /**
     * 执行一个无返回值的任务（Runnable）
     * 
     * 这个方法封装了 executor.execute(runnable) 的调用，
     * 调用方只需要传任务，不需要关心线程池的内部细节。
     * 
     * 使用示例：
     *   MyThreadPoolExecutor.execute(() -> {
     *       System.out.println("任务执行中...");
     *   });
     * 
     * @param task 要执行的任务（Runnable）
     */
    public static void execute(Runnable task) {
        EXECUTOR.execute(task);
    }

    /**
     * 获取线程池当前的运行状态
     * 
     * @return 包含池大小、活跃线程数、队列积压数的状态描述字符串
     */
    public static String getStatus() {
        return String.format(
                "poolSize=%d, activeCount=%d, queueSize=%d",
                EXECUTOR.getPoolSize(),
                EXECUTOR.getActiveCount(),
                EXECUTOR.getQueue().size()
        );
    }

    /**
     * 私有构造方法，防止外部实例化（工具类风格）
     * 
     * MyThreadPoolExecutor 是一个工具类，所有的功能都通过静态方法提供，
     * 所以不应该被实例化。
     */
    private MyThreadPoolExecutor() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    // ============================================================
    // 以下 main 方法用于单独测试线程池
    // 直接运行这个类就能看到效果，不需要启动 Spring Boot 项目
    // ============================================================
    public static void main(String[] args) {
        System.out.println("===== 开始测试 ThreadPoolExecutor =====");

        for (int i = 1; i <= 5; i++) {
            final int taskId = i;

            // 提交任务到线程池
            EXECUTOR.execute(() -> {
                String threadName = Thread.currentThread().getName();
                String now = LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));

                String msg = String.format(
                        "[%s] %s —— 任务 #%d 正在执行",
                        threadName,
                        now,
                        taskId
                );
                System.out.println(msg);

                // 模拟任务耗时
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        // 等待所有任务执行完毕
        EXECUTOR.shutdown();
        try {
            // 最多等待 10 秒
            if (EXECUTOR.awaitTermination(3, TimeUnit.SECONDS)) {
                System.out.println("===== 所有任务执行完毕 =====");
            } else {
                System.out.println("===== 超时，部分任务未执行完毕 =====");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}