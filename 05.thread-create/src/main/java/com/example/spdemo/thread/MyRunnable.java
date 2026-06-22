package com.example.spdemo.thread;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * ============================================================
 * 方式二：实现 Runnable 接口创建线程
 * ============================================================
 * 
 * 【核心思路】
 *   写一个类，让它 implements Runnable 接口，
 *   然后实现接口里的 run() 方法，把任务代码放进去。
 *   调用时：new Thread(new MyRunnable()).start()
 *   也就是把任务对象"扔"给一个 Thread，让 Thread 去启动。
 * 
 *   Runnable 是一个"函数式接口"（只有一个抽象方法 run()），
 *   所以也可以用 Lambda 表达式简化：
 *     new Thread(() -> { 任务代码 }).start();
 * 
 * 【对比方式一（继承 Thread）的改进】
 *   - 把"任务"和"线程"分开了：MyRunnable 只负责"做什么"，
 *     Thread 只负责"怎么启动线程" —— 这叫"解耦"
 *   - 因为 MyRunnable 是一个接口实现，所以这个类还可以再继承别的类，
 *     不受单继承限制
 *   - 同一个 MyRunnable 实例可以传给多个 Thread，实现资源共享
 * 
 * 【缺点】
 *   - run() 方法没有返回值，如果想知道任务执行结果，没办法
 *   - run() 方法不能 throws 受检异常（必须自己在内部 try-catch）
 * 
 * 【面试常见问题】
 *   Q: Runnable 和 Callable 的区别？
 *   A: Runnable 无返回值、不能抛异常；Callable 有返回值、能抛异常。
 *   Q: 继承 Thread 和实现 Runnable 哪个好？
 *   A: 实现 Runnable 好，解耦 + 不受单继承限制。
 * 
 * 【适合场景】
 *   日常开发中最常用的方式之一，尤其是配合 Lambda 表达式使用。
 */
public class MyRunnable implements Runnable {

    /**
     * 实现 Runnable 接口的 run() 方法
     * 当 new Thread(runnable).start() 后，新线程会执行这里的代码
     */
    @Override
    public void run() {
        // Thread.currentThread() 获取当前正在执行的线程对象
        // getName() 获取线程的名字
        String threadName = Thread.currentThread().getName();

        // 获取当前时间，格式化为 "HH:mm:ss.SSS"
        String now = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));

        // 组装要打印的信息
        String msg = String.format(
                "[%s] %s —— 方式二：实现 Runnable 接口",
                threadName,
                now
        );

        // 打印到控制台
        System.out.println(msg);
    }
}