-- ============================================================
-- Warm-Flow 工作流引擎教学示例 - 数据库初始化脚本（MySQL 8）
-- ============================================================
-- 使用方式：
--   mysql -uroot -p < init.sql
-- 或登录 mysql 后 source init.sql
--
-- 本脚本会：
--   1. 创建数据库 warm_flow_demo
--   2. 创建 Warm-Flow 引擎需要的 7 张表（flow_*）
--   3. 创建业务演示用的请假表（t_leave）
-- ============================================================

-- 1. 创建数据库（库不存在时创建，字符集 utf8mb4 支持 emoji 等 4 字节字符）
CREATE DATABASE IF NOT EXISTS `warm_flow_demo`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE `warm_flow_demo`;

-- ============================================================
-- 以下 7 张表是 Warm-Flow 引擎自身的表，务必保持字段与引擎一致，
-- 引擎通过雪花算法生成 id（应用侧填充），所以 id 不设自增。
-- 所有表都带 del_flag（逻辑删除）和 tenant_id（多租户）字段。
-- ============================================================

-- ------------------------------------------------------------
-- 1. flow_definition 流程定义表
--    记录流程的整体信息（编码、名称、版本、发布状态等）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `flow_definition` (
    `id`              BIGINT       NOT NULL COMMENT '主键id',
    `flow_code`       VARCHAR(40)  NOT NULL COMMENT '流程编码',
    `flow_name`       VARCHAR(100) NOT NULL COMMENT '流程名称',
    `model_value`     VARCHAR(40)  NOT NULL DEFAULT 'CLASSICS' COMMENT '设计器模型（CLASSICS经典模型 MIMIC仿钉钉模型）',
    `category`        VARCHAR(100) COMMENT '流程类别',
    `version`         VARCHAR(20)  NOT NULL COMMENT '流程版本',
    `is_publish`      TINYINT      NOT NULL DEFAULT 0 COMMENT '是否发布（0未发布 1已发布 9失效）',
    `form_custom`     CHAR(1)      DEFAULT 'N' COMMENT '审批表单是否自定义（Y是 N否）',
    `form_path`       VARCHAR(100) COMMENT '审批表单路径',
    `activity_status` TINYINT      NOT NULL DEFAULT 1 COMMENT '流程激活状态（0挂起 1激活）',
    `listener_type`   VARCHAR(100) COMMENT '监听器类型',
    `listener_path`   VARCHAR(400) COMMENT '监听器路径',
    `ext`             VARCHAR(500) COMMENT '业务详情 存业务表对象json字符串',
    `xml_string`      VARCHAR(500) COMMENT '流程定义的xml字符串',
    `create_time`     DATETIME     COMMENT '创建时间',
    `update_time`     DATETIME     COMMENT '更新时间',
    `del_flag`        CHAR(1)      DEFAULT '0' COMMENT '删除标志',
    `tenant_id`       VARCHAR(40)  COMMENT '租户id',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程定义表';

-- ------------------------------------------------------------
-- 2. flow_node 流程节点表
--    记录流程中的各个节点信息（节点类型、权限标识、监听器等）
--    node_type：0开始 1中间 2结束 3互斥网关 4并行网关
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `flow_node` (
    `id`              BIGINT        NOT NULL COMMENT '主键id',
    `node_type`       TINYINT       NOT NULL COMMENT '节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）',
    `definition_id`   BIGINT        NOT NULL COMMENT '流程定义id',
    `node_code`       VARCHAR(100)  NOT NULL COMMENT '流程节点编码',
    `node_name`       VARCHAR(100)  COMMENT '流程节点名称',
    `permission_flag` VARCHAR(200)  COMMENT '权限标识（权限类型:权限标识，可以多个，用@@隔开）',
    `node_ratio`      DECIMAL(6,3)  COMMENT '流程签署比例值（0或签 0-100票签 100会签）',
    `coordinate`      VARCHAR(100)  COMMENT '坐标',
    `skip_any_node`   VARCHAR(100)  COMMENT '任意结点跳转',
    `listener_type`   VARCHAR(100)  COMMENT '监听器类型',
    `listener_path`   VARCHAR(400)  COMMENT '监听器路径',
    `handler_type`    VARCHAR(100)  COMMENT '处理器类型',
    `handler_path`    VARCHAR(400)  COMMENT '处理器路径',
    `form_custom`     CHAR(1)       DEFAULT 'N' COMMENT '审批表单是否自定义（Y是 N否）',
    `form_path`       VARCHAR(100)  COMMENT '审批表单路径',
    `version`         VARCHAR(20)   NOT NULL COMMENT '版本',
    `create_time`     DATETIME      COMMENT '创建时间',
    `update_time`     DATETIME      COMMENT '更新时间',
    `ext`             TEXT          COMMENT '节点扩展属性',
    `del_flag`        CHAR(1)       DEFAULT '0' COMMENT '删除标志',
    `tenant_id`       VARCHAR(40)   COMMENT '租户id',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程节点表';

-- ------------------------------------------------------------
-- 3. flow_skip 节点跳转关联表
--    记录节点之间的跳转关系（当前节点→下一个节点）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `flow_skip` (
    `id`              BIGINT       NOT NULL COMMENT '主键id',
    `definition_id`   BIGINT       NOT NULL COMMENT '流程定义id',
    `node_id`         BIGINT       COMMENT '节点id',
    `now_node_code`   VARCHAR(100) NOT NULL COMMENT '当前流程节点的编码',
    `now_node_type`   TINYINT      COMMENT '当前节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）',
    `next_node_code`  VARCHAR(100) NOT NULL COMMENT '下一个流程节点的编码',
    `next_node_type`  TINYINT      COMMENT '下一个节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）',
    `skip_name`       VARCHAR(100) COMMENT '跳转名称',
    `skip_type`       VARCHAR(40)  COMMENT '跳转类型（PASS审批通过 REJECT退回）',
    `skip_condition`  VARCHAR(200) COMMENT '跳转条件',
    `coordinate`      VARCHAR(100) COMMENT '坐标',
    `create_time`     DATETIME     COMMENT '创建时间',
    `update_time`     DATETIME     COMMENT '更新时间',
    `del_flag`        CHAR(1)      DEFAULT '0' COMMENT '删除标志',
    `tenant_id`       VARCHAR(40)  COMMENT '租户id',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='节点跳转关联表';

-- ------------------------------------------------------------
-- 4. flow_instance 流程实例表
--    记录每一次流程实例的信息（当前节点、流程状态、业务id等）
--    flow_status：0待提交 1审批中 2审批通过 4终止 5作废 6撤销
--                 8已完成 9已退回 10失效 11拿回
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `flow_instance` (
    `id`              BIGINT       NOT NULL COMMENT '主键id',
    `definition_id`   BIGINT       NOT NULL COMMENT '对应flow_definition表的id',
    `flow_name`       VARCHAR(100) COMMENT '流程名称',
    `business_id`     VARCHAR(40)  NOT NULL COMMENT '业务id',
    `node_type`       TINYINT      NOT NULL COMMENT '节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）',
    `node_code`       VARCHAR(40)  NOT NULL COMMENT '流程节点编码',
    `node_name`       VARCHAR(100) COMMENT '流程节点名称',
    `variable`        TEXT         COMMENT '任务变量',
    `flow_status`     VARCHAR(20)  NOT NULL COMMENT '流程状态（0待提交 1审批中 2审批通过 4终止 5作废 6撤销 8已完成 9已退回 10失效 11拿回）',
    `activity_status` TINYINT      NOT NULL DEFAULT 1 COMMENT '流程激活状态（0挂起 1激活）',
    `def_json`        TEXT         COMMENT '流程定义json',
    `create_by`       VARCHAR(64)  COMMENT '创建者',
    `form_custom`     CHAR(1)      DEFAULT 'N' COMMENT '审批表单是否自定义（Y是 N否）',
    `form_path`       VARCHAR(100) COMMENT '审批表单路径',
    `create_time`     DATETIME     COMMENT '创建时间',
    `update_time`     DATETIME     COMMENT '更新时间',
    `ext`             VARCHAR(500) COMMENT '扩展字段，预留给业务系统使用',
    `del_flag`        CHAR(1)      DEFAULT '0' COMMENT '删除标志',
    `tenant_id`       VARCHAR(40)  COMMENT '租户id',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程实例表';

-- ------------------------------------------------------------
-- 5. flow_task 待办任务表
--    记录当前待办任务信息（谁来办、当前在哪个节点）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `flow_task` (
    `id`            BIGINT       NOT NULL COMMENT '主键id',
    `definition_id` BIGINT       NOT NULL COMMENT '对应flow_definition表的id',
    `instance_id`   BIGINT       NOT NULL COMMENT '对应flow_instance表的id',
    `flow_name`     VARCHAR(100) COMMENT '流程名称',
    `business_id`   VARCHAR(40)  COMMENT '业务id',
    `node_code`     VARCHAR(100) NOT NULL COMMENT '节点编码',
    `node_name`     VARCHAR(100) COMMENT '节点名称',
    `node_type`     TINYINT      NOT NULL COMMENT '节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）',
    `permission_list` VARCHAR(2000) COMMENT '权限标识列表',
    -- 注意：warm-flow 1.3.x 起 FlowTask 实体不再维护 flow_status，列保留并设默认值兼容旧数据
    `flow_status`   VARCHAR(20)  NOT NULL DEFAULT '1' COMMENT '流程状态（0待提交 1审批中 2审批通过 4终止 5作废 6撤销 8已完成 9已退回 10失效 11拿回）',
    `form_custom`   CHAR(1)      DEFAULT 'N' COMMENT '审批表单是否自定义（Y是 N否）',
    `form_path`     VARCHAR(100) COMMENT '审批表单路径',
    `create_time`   DATETIME     COMMENT '创建时间',
    `update_time`   DATETIME     COMMENT '更新时间',
    `del_flag`      CHAR(1)      DEFAULT '0' COMMENT '删除标志',
    `tenant_id`     VARCHAR(40)  COMMENT '租户id',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='待办任务表';

-- ------------------------------------------------------------
-- 6. flow_his_task 历史任务记录表
--    记录已完成的任务，用于追溯审批过程
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `flow_his_task` (
    `id`                BIGINT       NOT NULL COMMENT '主键id',
    `definition_id`     BIGINT       NOT NULL COMMENT '对应flow_definition表的id',
    `instance_id`       BIGINT       NOT NULL COMMENT '对应flow_instance表的id',
    `task_id`           BIGINT       NOT NULL COMMENT '对应flow_task表的id',
    `flow_name`         VARCHAR(100) COMMENT '流程名称',
    `business_id`       VARCHAR(40)  COMMENT '业务id',
    `node_code`         VARCHAR(100) COMMENT '开始节点编码',
    `node_name`         VARCHAR(100) COMMENT '开始节点名称',
    `node_type`         TINYINT      COMMENT '开始节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）',
    `target_node_code`  VARCHAR(200) COMMENT '目标节点编码',
    `target_node_name`  VARCHAR(200) COMMENT '结束节点名称',
    `approver`          VARCHAR(40)  COMMENT '审批者',
    `cooperate_type`    TINYINT      NOT NULL DEFAULT 0 COMMENT '协作方式(1审批 2转办 3委派 4会签 5票签 6加签 7减签)',
    `collaborator`      VARCHAR(40)  COMMENT '协作人',
    `permission_list`   VARCHAR(2000) COMMENT '权限标识列表',
    `skip_type`         VARCHAR(10)  NOT NULL COMMENT '流转类型（PASS通过 REJECT退回 NONE无动作）',
    `flow_status`       VARCHAR(20)  NOT NULL COMMENT '流程状态（0待提交 1审批中 2审批通过 4终止 5作废 6撤销 8已完成 9已退回 10失效 11拿回）',
    `form_custom`       CHAR(1)      DEFAULT 'N' COMMENT '审批表单是否自定义（Y是 N否）',
    `form_path`         VARCHAR(100) COMMENT '审批表单路径',
    `message`           VARCHAR(500) COMMENT '审批意见',
    `variable`          TEXT         COMMENT '任务变量',
    `ext`               TEXT         COMMENT '业务详情 存业务表对象json字符串',
    `create_by`         VARCHAR(64)  COMMENT '创建者',
    `create_time`       DATETIME     COMMENT '任务开始时间',
    `update_time`       DATETIME     COMMENT '审批完成时间',
    `del_flag`          CHAR(1)      DEFAULT '0' COMMENT '删除标志',
    `tenant_id`         VARCHAR(40)  COMMENT '租户id',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='历史任务记录表';

-- ------------------------------------------------------------
-- 7. flow_user 流程用户表
--    记录任务可以由谁办理（审批人/转办人/委托人）
--    type：1审批人权限 2转办人权限 3委托人权限
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `flow_user` (
    `id`            BIGINT      NOT NULL COMMENT '主键id',
    `type`          CHAR(1)     NOT NULL COMMENT '人员类型（1待办任务的审批人权限 2待办任务的转办人权限 3待办任务的委托人权限）',
    `processed_by`  VARCHAR(80) COMMENT '权限人',
    `associated`    BIGINT      NOT NULL COMMENT '任务表id',
    `create_time`   DATETIME    COMMENT '创建时间',
    `create_by`     VARCHAR(80) COMMENT '创建人',
    `update_time`   DATETIME    COMMENT '更新时间',
    `del_flag`      CHAR(1)     DEFAULT '0' COMMENT '删除标志',
    `tenant_id`     VARCHAR(40) COMMENT '租户id',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程用户表';

-- ============================================================
-- 8. t_leave 业务请假表（业务系统自己的表，教学演示用）
--    演示「业务数据」如何与「流程引擎数据」解耦关联：
--       instance_id  -> 关联 flow_instance 的 id
--       node_code    -> 当前流程节点编码（组长审批/经理审批）
--       flow_status  -> 业务侧冗余的流程状态，便于业务查询
-- ============================================================
CREATE TABLE IF NOT EXISTS `t_leave` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `applicant`   VARCHAR(64)  NOT NULL COMMENT '请假人',
    `days`        INT          NOT NULL COMMENT '请假天数',
    `reason`      VARCHAR(255) COMMENT '请假事由',
    `instance_id` BIGINT       COMMENT '关联的流程实例id（flow_instance.id）',
    `node_code`   VARCHAR(100) COMMENT '当前流程节点编码',
    `node_name`   VARCHAR(100) COMMENT '当前流程节点名称',
    `flow_status` VARCHAR(20)  COMMENT '业务冗余的流程状态（与 flow_instance.flow_status 同步）',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='请假业务表';
