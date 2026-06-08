package com.example.spdemo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class ExecutorConfig {

    @Bean("asyncExecutor")
    public Executor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心线程数：一直存活的线程数。默认值是 1。
        // 你的 Demo 是轻量任务，1 就够；如果并发量大可以调到 CPU 核心数。
        executor.setCorePoolSize(2);

        // 最大线程数：忙的时候最多撑到多少个线程。默认值是 Integer.MAX_VALUE（危险！）。
        // 这里设为 4，够你的小 Demo 玩了。
        executor.setMaxPoolSize(4);

        // 队列容量：核心线程忙不过来时，新任务先进队列排队。默认是 Integer.MAX_VALUE（也危险）。
        // 设为 50 就够了，超出后才会创建新线程直到 maxPoolSize。
        executor.setQueueCapacity(50);

        // 线程名前缀：这样你在日志/控制台就能一眼认出是哪个池的线程。
        executor.setThreadNamePrefix("async-demo-");

        // 拒绝策略：队列满了、线程也满了，新任务怎么处理？
        // CallerRunsPolicy：让提交任务的那个线程（也就是调用方）自己执行，不会丢任务。
        // 默认是 AbortPolicy（直接抛异常），新手很容易踩这个坑。
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 非核心线程空闲超时时间（秒），超过后回收。
        executor.setKeepAliveSeconds(60);

        // 允许核心线程也超时回收（默认 false，核心线程永不回收）。
        executor.setAllowCoreThreadTimeOut(false);

        // 初始化线程池（create 线程）
        executor.initialize();

        return executor;
    }
}
