package com.example.consumer;

import com.example.greeting.GreetingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * ====================================================================
 * 【消费方控制器 —— GreetingController】
 *
 * 演示如何注入并使用 Starter 提供的 GreetingService。
 *
 * 关键点：
 *   GreetingService 来自 com.example.greeting 包（Starter 模块），
 *   它是由自动配置类创建并注册到 Spring 容器的。
 *   这里我们通过 @Autowired 直接注入，完全不需要在 Consumer 模块
 *   的代码中手动创建 GreetingService 对象——这就是 Starter 的承诺：
 *   "添加依赖 + 配置属性 = 开箱即用"。
 * ====================================================================
 */
@RestController
public class GreetingController {

    // ================================================================
    // 注入 Starter 提供的 GreetingService
    //
    // @Autowired：
    //   Spring 会按照类型（GreetingService）从容器中查找匹配的 Bean，
    //   并自动赋值给此字段。
    //
    // 思考题：如果容器中有多个 GreetingService 类型的 Bean 怎么办？
    // 答：可以用 @Qualifier("beanName") 指定具体哪一个。
    // 但在本例中，自动配置类只创建了一个，所以无需担心。
    // ================================================================
    @Autowired
    private GreetingService greetingService;

    /**
     * GET 接口：/greet?name=World
     *
     * 调用 Starter 的 greet() 方法，返回定制化的问候语。
     * 例如：访问 http://localhost:8080/greet?name=SpringBoot
     * 结果：Hello, SpringBoot! （取决于 application.properties 中的配置）
     *
     * @RequestParam 从 URL 查询参数中读取 name，默认值为 "World"
     *
     * @param name 要打招呼的对象名称
     * @return 问候语字符串
     */
    @GetMapping("/greet")
    public String greet(@RequestParam(value = "name", defaultValue = "World") String name) {
        // 直接调用 Starter 提供的服务方法
        return greetingService.greet(name);
    }
}