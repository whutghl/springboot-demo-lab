package com.example.spdemo.controller;

import com.example.spdemo.thread.MyCallable;
import com.example.spdemo.thread.MyRunnable;
import com.example.spdemo.thread.MyThread;
import com.example.spdemo.thread.MyThreadPoolExecutor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.FutureTask;

/**
 * ============================================================
 * 线程 4 种创建方式演示的 Controller
 * ============================================================
 * 
 * 这是一个 Spring Boot 的 RESTful 接口控制器，
 * 提供了 4 个 GET 接口，分别演示 4 种创建线程的方式。
 * 
 * 访问任意一个接口，对应的线程就会被创建并执行，
 * 你可以在控制台（终端）看到线程输出的信息。
 * 
 * ============================================================
 * ⭐ 推荐使用方式四（ThreadPoolExecutor）
 * ============================================================
 * 
 * 理由简要说明：
 *   1. 线程池复用线程，避免频繁创建/销毁的开销
 *   2. 通过核心线程数、最大线程数、有界队列控制并发量，防止 OOM
 *   3. 任务提交与执行策略解耦，代码更清晰
 *   4. 支持优雅关闭（shutdown）、任务超时、拒绝策略等高级功能
 * 
 * 详细原因见项目根目录的 guide.md
 */
@RestController
@RequestMapping("/api/thread")  // 所有接口的路径都以 /api/thread 开头
public class ThreadController {

    /**
     * 方式一：继承 Thread 类
     * 
     * 使用步骤：
     *   1. new MyThread() 创建一个线程对象
     *   2. 调用 start() 方法启动线程
     * 
     * 注意：千万不能调 run()！run() 只是普通方法调用，不会启动新线程。
     * 
     * 请求示例：GET http://localhost:10000/api/thread/thread
     * 
     * @return 包含提示信息的 JSON
     */
    @GetMapping("/thread")
    public Map<String, Object> demoThread() {
        // 创建 MyThread 对象（MyThread 继承了 Thread）
        MyThread t = new MyThread();

        // 调用 start() 启动新线程，JVM 会自动调用 MyThread 的 run() 方法
        t.start();

        // 返回给前端的提示信息
        return ok("已提交 —— 方式一：继承 Thread 类");
    }

    /**
     * 方式二：实现 Runnable 接口
     * 
     * 使用步骤：
     *   1. new MyRunnable() 创建一个任务对象
     *   2. new Thread(任务对象) 把任务传给线程
     *   3. .start() 启动线程
     * 
     * 因为 Runnable 是函数式接口，也可以简写为 Lambda：
     *   new Thread(() -> System.out.println("hello")).start();
     * 
     * 请求示例：GET http://localhost:10000/api/thread/runnable
     * 
     * @return 包含提示信息的 JSON
     */
    @GetMapping("/runnable")
    public Map<String, Object> demoRunnable() {
        // 创建 Runnable 实现类对象（任务）
        MyRunnable task = new MyRunnable();

        // 把任务传给 Thread 构造函数，然后启动
        new Thread(task).start();

        return ok("已提交 —— 方式二：实现 Runnable 接口");
    }

    /**
     * 方式三：实现 Callable + FutureTask
     * 
     * 使用步骤：
     *   1. new MyCallable() 创建一个 Callable 任务对象
     *   2. new FutureTask<>(callable) 创建 FutureTask 作为"桥梁"
     *   3. new Thread(futureTask).start() 启动线程
     *   4. futureTask.get() 阻塞等待并获取返回值
     * 
     * 注意：get() 会阻塞当前线程（这里是处理 HTTP 请求的 Tomcat 线程），
     * 直到异步任务执行完毕。如果任务耗时较长，HTTP 请求也会等很久。
     * 这里只是演示，实际项目中一般会把 get() 放在业务线程中处理。
     * 
     * 请求示例：GET http://localhost:10000/api/thread/callable
     * 
     * @return Callable 的返回结果
     * @throws Exception 如果 call() 抛异常，get() 会把异常包装成 ExecutionException 抛出
     */
    @GetMapping("/callable")
    public Map<String, Object> demoCallable() throws Exception {
        // 第一步：创建 Callable 任务对象
        MyCallable callable = new MyCallable();

        // 第二步：创建 FutureTask，把 Callable 传进去
        // FutureTask 同时实现了 Runnable 和 Future 接口，
        // 所以它可以传给 Thread（因为 Thread 接受 Runnable），
        // 同时还能通过 get() 获取返回值（因为它是 Future）
        FutureTask<String> futureTask = new FutureTask<>(callable);

        // 第三步：启动线程
        new Thread(futureTask).start();

        // 第四步：获取返回值
        // get() 会阻塞等待，直到 MyCallable.call() 执行完毕并返回结果
        // 如果不希望无限等待，可以用 get(3, TimeUnit.SECONDS) 设置超时
        String result = futureTask.get();

        // 把结果返回给前端
        return ok(result);
    }

    /**
     * 方式四：ExecutorService 线程池（ThreadPoolExecutor）
     * 
     * 使用步骤：
     *   1. 提前创建好 ThreadPoolExecutor 实例（见上面的 executor 变量）
     *   2. executor.execute(任务) 或 executor.submit(任务) 提交任务
     *       execute(Runnable)     —— 提交无返回值的任务
     *       submit(Callable)      —— 提交有返回值的任务，返回 Future
     *       submit(Runnable)      —— 提交无返回值的任务，但也返回 Future（get() 返回 null）
     *   3. 程序结束时调用 executor.shutdown() 优雅关闭
     * 
     * 这里使用 Lambda 表达式直接写任务代码，省去了单独创建类的步骤。
     * 
     * 注意：这里直接引用 MyThreadPoolExecutor.EXECUTOR，
     * 线程池的定义和配置已经提取到 MyThreadPoolExecutor.java 中。
     * 
     * 请求示例：GET http://localhost:10000/api/thread/pool
     * 
     * @return 包含提示信息和线程池当前状态的 JSON
     */
    @GetMapping("/pool")
    public Map<String, Object> demoPool() {
        // 提交任务到线程池
        // MyThreadPoolExecutor.execute(Runnable task) 方法会把任务交给线程池管理
        // 线程池会根据当前线程数、队列状况决定何时执行
        MyThreadPoolExecutor.execute(() -> {
            // 这里的代码就是一个 Runnable 任务，由线程池中的某个线程执行
            String threadName = Thread.currentThread().getName();
            String now = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));

            String msg = String.format(
                    "[%s] %s —— 方式四：ExecutorService 线程池 (ThreadPoolExecutor)",
                    threadName,
                    now
            );
            System.out.println(msg);
        });

        // 组装返回给前端的数据
        // 除了提示信息，还返回线程池的当前状态，方便观察
        Map<String, Object> data = new HashMap<>();
        data.put("message", "已提交 —— 方式四：ExecutorService 线程池 (ThreadPoolExecutor)");
        data.put("poolSize", MyThreadPoolExecutor.EXECUTOR.getPoolSize());        // 当前线程池中的线程数
        data.put("activeCount", MyThreadPoolExecutor.EXECUTOR.getActiveCount());  // 正在执行任务的线程数
        data.put("queueSize", MyThreadPoolExecutor.EXECUTOR.getQueue().size());   // 队列中等待的任务数

        return data;
    }

    /**
     * 辅助方法：生成统一的返回格式
     * 
     * @param msg 提示信息
     * @return {"code": 200, "message": "xxx"}
     */
    private Map<String, Object> ok(String msg) {
        Map<String, Object> m = new HashMap<>();
        m.put("code", 200);
        m.put("message", msg);
        return m;
    }
}