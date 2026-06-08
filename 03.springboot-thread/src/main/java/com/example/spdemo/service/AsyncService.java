package com.example.spdemo.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class AsyncService {

    /**
     * @Async 会让这个方法在线程池"asyncExecutor"中异步执行。
     * 返回值用 CompletableFuture 包一下，调用方可以拿到结果（也可以不用等）。
     */
    @Async("asyncExecutor")
    public CompletableFuture<String> doAsyncWork() {
        // 打印当前线程名，方便你在控制台验证线程池是否生效
        String threadName = Thread.currentThread().getName();
        System.out.println("[异步任务] 执行线程: " + threadName);

        // 模拟一个耗时操作（比如调第三方接口、写文件等）
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("[异步任务] 完成! 线程: " + threadName);
        return CompletableFuture.completedFuture("异步任务完成，线程: " + threadName);
    }

    /**
     * 不加 @Async，对比用——能看到它在哪个线程上跑。
     */
    public String doSyncWork() {
        String threadName = Thread.currentThread().getName();
        System.out.println("[同步任务] 执行线程: " + threadName);
        return "同步任务完成，线程: " + threadName;
    }
}
