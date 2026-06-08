package com.example.spdemo.controller;

import com.example.spdemo.service.AsyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api")
public class HelloController {

    @Autowired
    private AsyncService asyncService;

    @GetMapping("/hello")
    public Map<String, Object> hello() {
        // 打印调用方线程名（应该是 http-nio-xxx，即 Tomcat 线程）
        String callerThread = Thread.currentThread().getName();
        System.out.println("[Controller] 收到请求，当前线程: " + callerThread);

        // 调用异步方法——不会阻塞，立刻返回
        CompletableFuture<String> asyncResult = asyncService.doAsyncWork();

        // 顺便调同步方法做对比
        String syncResult = asyncService.doSyncWork();

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "Hello, Spring Boot 2.7!");

        // 等异步结果拿到后一起返回（实际生产中可以不等，这里教学用）
        String asyncVal = asyncResult.join(); // join() 会阻塞等结果  阻塞当前线程，直到异步任务完成
        result.put("async", asyncVal);
        result.put("sync", syncResult);
        result.put("callerThread", callerThread);

        return result;
    }
}
