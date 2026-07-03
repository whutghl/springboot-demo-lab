package com.example.greeting;

/**
 * ====================================================================
 * 【服务类 —— GreetingService】
 *
 * 作用：
 *   这是本 Starter 对外提供的核心业务能力——
 *   根据传入的名字，返回一条组装好的问候语。
 *
 * 设计理念：
 *   一个 Starter 的核心价值在于"开箱即用"地提供某种功能。
 *   使用者只需注入 GreetingService，无需关心其内部的配置细节。
 *   这就是"自动配置"（Auto-Configuration）的终极目标。
 *
 * 这里我们没有使用任何框架注解（@Component 等），
 * 因为该 Bean 将由自动配置类（GreetingAutoConfiguration）来创建，
 * 这更符合 Starter 的"解耦"思想：
 *   - Starter 模块负责提供 "零件"（本服务类）
 *   - 自动配置类负责 "组装"（new 对象并注入依赖）
 * ====================================================================
 */
public class GreetingService {

    // ================================================================
    // GreetingProperties 的引用
    // 该对象由自动配置类创建并传入，里面包含了用户的自定义配置值。
    // ================================================================
    private final GreetingProperties properties;

    /**
     * 构造方法 —— 由自动配置类调用，传入配置对象。
     *
     * @param properties 配置属性对象（已由 Spring 绑定好用户配置值）
     */
    public GreetingService(GreetingProperties properties) {
        this.properties = properties;
    }

    /**
     * ================================================================
     * 核心业务方法：打招呼
     *
     * 逻辑：
     *   将配置中的 prefix + name + suffix 拼接成一个完整的句子。
     *
     * 举例：
     *   - 配置：mygreeting.prefix=Hello, mygreeting.suffix=!
     *   - 调用：greet("World")
     *   - 结果："Hello, World!"
     *
     * @param name 要打招呼的对象名称
     * @return 组装后的问候语句
     * ================================================================
     */
    public String greet(String name) {
        // 从配置对象中读取前缀和后缀
        String prefix = properties.getPrefix();
        String suffix = properties.getSuffix();

        // 组装并返回问候语（注意中间加了一个 ", " 让句子更通顺）
        return prefix + ", " + name + suffix;
    }
}