package com.example.spdemo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.sql.Timestamp;

/**
 * 用户实体，对应 Flyway 迁移脚本建出来的 t_user 表。
 *
 * Lombok 注解说明：
 *   @Data = @Getter + @Setter + @ToString + @EqualsAndHashCode + @RequiredArgsConstructor，
 *   一句话消灭所有 getter/setter 样板代码（这正是 Lombok 的用途）。
 *
 * MyBatis-Plus 注解说明（表结构与 Flyway 一一对应）：
 *   @TableName("t_user")  → 指定本实体映射哪张表
 *   @TableId              → 指定主键（IdType.AUTO 对应 V1 脚本的 AUTO_INCREMENT）
 *   @TableField           → 下划线列名与驼峰属性名不一致时的显式映射
 *
 * 教学重点：表结构是 Flyway 定义的，MyBatis-Plus 只负责 CRUD。
 *   表结构变更 = 写新的迁移脚本（V4、V5...），再同步调整本实体即可。
 */
@Data
@TableName("t_user")
public class User {

    /** 主键，对应 V1 脚本的 id BIGINT AUTO_INCREMENT */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户姓名，对应 V1 脚本的 name 列 */
    private String name;

    /** 邮箱，对应 V1 脚本的 email 列 */
    private String email;

    /** 手机号，对应 V2 脚本新增的 phone 列（演示「表结构演进」在 Java 侧的体现） */
    private String phone;

    /** 创建时间，对应 V1 脚本的 created_at 列 */
    @TableField("created_at")
    private Timestamp createdAt;
}
