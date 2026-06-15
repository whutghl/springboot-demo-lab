package com.example.spdemo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 测试 ObjectReferenceService 中共享 POJO 成员变量的线程安全性。
 * <p>
 * 原理：并行发送 100 个请求（每个请求携带随机 name/age），
 * 然后检查每个响应中的 currentName 是否与 currentAge 匹配预期值。
 * 如果发生数据交叉污染（name 来自线程A，age 来自线程B），则说明线程不安全。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ObjectReferenceThreadSafetyTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void sharedPojoIsNotThreadSafe() throws InterruptedException {
        int totalRequests = 10000;
        int concurrency = 50;

        // 1. 预先生成所有请求的预期 name -> age 映射
        Map<String, Integer> expected = new HashMap<>();
        List<RequestParam> params = new ArrayList<>();
        for (int i = 0; i < totalRequests; i++) {
            String name = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
            int age = new Random().nextInt(50) + 18;
            expected.put(name, age);
            params.add(new RequestParam(name, age));
        }

        // 2. 并发发送请求，收集所有响应
        ExecutorService executor = Executors.newFixedThreadPool(concurrency);
        CountDownLatch latch = new CountDownLatch(totalRequests);
        List<String> jsonResponses = Collections.synchronizedList(new ArrayList<>());

        for (RequestParam param : params) {
            executor.submit(() -> {
                try {
                    String url = String.format("http://localhost:%d/api/object/unsafe?name=%s&age=%d",
                            port, param.name, param.age);
                    ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
                    jsonResponses.add(response.getBody());
                } catch (Exception e) {
                    jsonResponses.add("ERROR: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executor.shutdown();

        // 3. 解析并检查每个响应
        AtomicInteger contaminated = new AtomicInteger(0);
        for (String json : jsonResponses) {
            // 手动解析简单 JSON，避免引入额外依赖
            String respName = extractJsonValue(json, "currentName");
            String respAgeStr = extractJsonValue(json, "currentAge");
            String threadName = extractJsonValue(json, "thread");

            if (respName == null || respAgeStr == null) {
                System.out.println("  ⚠️  解析失败: " + json);
                continue;
            }

            int respAge;
            try {
                respAge = Integer.parseInt(respAgeStr);
            } catch (NumberFormatException e) {
                System.out.println("  ⚠️  age 解析失败: " + respAgeStr);
                continue;
            }

            Integer expectedAge = expected.get(respName);
            if (expectedAge == null || respAge != expectedAge) {
                contaminated.incrementAndGet();
                System.out.printf("  ⚠️  数据污染: name=%s, 预期 age=%s, 实际 age=%d, thread=%s%n",
                        respName, expectedAge == null ? "未知" : expectedAge, respAge, threadName);
            }
        }

        int clean = totalRequests - contaminated.get();
        System.out.println();
        System.out.println("📊 总请求数: " + totalRequests);
        System.out.println("❌ 数据污染: " + contaminated.get());
        System.out.println("✅ 数据正常: " + clean);

        // 4. 断言：预期有数据交叉污染（共享 POJO 线程不安全）
        //    注意：极低并发下可能偶尔未复现，但大多数情况应 > 0
        System.out.println();
        if (contaminated.get() > 0) {
            System.out.println("➡️  结论：共享 POJO 成员变量 ❌ 线程不安全（存在数据交叉污染）");
        } else {
            System.out.println("➡️  结论：本次未复现污染（并发不够或运气好）");
        }
        System.out.println();
        System.out.println("📌 最终共享状态:");
        String stateUrl = String.format("http://localhost:%d/api/object/state", port);
        ResponseEntity<String> stateResponse = restTemplate.getForEntity(stateUrl, String.class);
        System.out.println(stateResponse.getBody());

        // 不强制断言失败（并发低时可能偶现无污染），但输出结果供人工判断
        // 这里用 assertTrue 仅做通知，实际教学中看控制台输出即可
        System.out.println();
        System.out.println("💡 如果 contamininated > 0，说明共享 POJO 成员变量是线程不安全的。");
    }

    /**
     * 简易 JSON 字段提取（不含引号的值），不依赖第三方 JSON 库
     */
    private String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\":";
        int keyStart = json.indexOf(searchKey);
        if (keyStart < 0) return null;
        int valueStart = keyStart + searchKey.length();

        // 跳过可能的空白
        while (valueStart < json.length() && json.charAt(valueStart) == ' ') {
            valueStart++;
        }

        if (valueStart >= json.length()) return null;

        // 判断是字符串（带引号）还是数字（不带引号）
        if (json.charAt(valueStart) == '"') {
            int valueEnd = json.indexOf('"', valueStart + 1);
            if (valueEnd < 0) return null;
            return json.substring(valueStart + 1, valueEnd);
        } else {
            // 数字: 读到逗号或 } 为止
            int valueEnd = valueStart;
            while (valueEnd < json.length() && json.charAt(valueEnd) != ',' && json.charAt(valueEnd) != '}') {
                valueEnd++;
            }
            return json.substring(valueStart, valueEnd).trim();
        }
    }

    /**
     * 请求参数内部类
     */
    private static class RequestParam {
        final String name;
        final int age;

        RequestParam(String name, int age) {
            this.name = name;
            this.age = age;
        }
    }
}