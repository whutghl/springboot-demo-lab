-- ============================================================
-- V1：初始化用户表（MySQL 8 方言）
-- ============================================================
-- 文件名规范：V<版本号>__<描述>.sql（两个下划线）
--   - 版本号必须是纯数字，Flyway 按版本号升序执行，只会执行比当前库新的脚本
--   - 本脚本创建 t_user 表（避免使用 user，它是 SQL 保留字）
--   - V1 通常对应「首次建库/建表」，之后每个版本的变更都必须新增脚本，
--     绝不回头修改已执行过的旧脚本（改了会触发 checksum 校验失败，见 guide.md）
--
-- MySQL 方言要点（对比此前 H2 教学版）：
--   AUTO_INCREMENT     ：自增主键（H2 同款关键字，通用）
--   ENGINE=InnoDB      ：InnoDB 事务引擎（MySQL 默认，显式写出便于教学）
--   DEFAULT CHARSET    ：utf8mb4（MySQL 8 推荐的完整 UTF-8，支持 emoji 等 4 字节字符）
CREATE TABLE t_user (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    name       VARCHAR(64)  NOT NULL COMMENT '用户姓名',
    email      VARCHAR(128) NOT NULL COMMENT '邮箱',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
