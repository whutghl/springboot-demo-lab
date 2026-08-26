package com.example.spdemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot 启动类（Flyway 教学版）
 *
 * 启动时序（Spring Boot 自动完成的）：
 *   1. 创建数据源（MySQL 8）
 *   2. Flyway 检查 db/migration 目录，按版本号顺序执行未执行的脚本
 *      （首次启动会依次执行 V1 → V2 → V3，并把执行记录写入 flyway_schema_history 表）
 *   3. 迁移成功后才开始启动 Web 容器，保证「表结构就绪」先于「业务接口可用」
 */
@SpringBootApplication
public class SpdemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpdemoApplication.class, args);
    }
}
