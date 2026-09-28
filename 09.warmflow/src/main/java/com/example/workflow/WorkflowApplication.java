package com.example.workflow;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot 启动类（Warm-Flow 工作流引擎教学版）
 *
 * 启动时序：
 *   1. 创建数据源（MySQL 8，库 warm_flow_demo 由 init.sql 创建）
 *   2. warm-flow-mybatis-plus-sb-starter 自动配置生效，注册引擎核心 Service Bean
 *      （DefService / InsService / TaskService / HisTaskService 等，无需手动注册）
 *   3. FlowInitRunner（CommandLineRunner）在启动时自动导入并发布「请假审批流程」定义
 *   4. Web 容器启动，业务接口可用
 *
 * @MapperScan 扫描业务 Mapper（LeaveMapper），与 warm-flow 引擎自带的 Mapper 互不干扰。
 */
@SpringBootApplication
@MapperScan("com.example.workflow.mapper")
public class WorkflowApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkflowApplication.class, args);
    }
}
