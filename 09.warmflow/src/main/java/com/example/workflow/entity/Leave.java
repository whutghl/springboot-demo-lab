package com.example.workflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.sql.Timestamp;

/**
 * 请假业务实体，对应业务表 t_leave。
 *
 * 教学重点：业务数据（请假单）与流程数据（flow_instance / flow_task）是「解耦」的两套数据，
 * 通过 instance_id 关联。业务表冗余了 node_code / node_name / flow_status 三个字段，
 * 方便业务侧直接查询「这条请假单当前走到哪、状态如何」，而不用每次 join 引擎表。
 *
 * 字段与 t_leave 一一对应，由 MyBatis-Plus 负责 CRUD（表结构在 init.sql 中创建）。
 */
@Data
@TableName("t_leave")
public class Leave {

    /** 主键，AUTO_INCREMENT */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 请假人 */
    private String applicant;

    /** 请假天数 */
    private Integer days;

    /** 请假事由 */
    private String reason;

    /** 关联的流程实例 id（flow_instance.id），启动流程后回填 */
    @TableField("instance_id")
    private Long instanceId;

    /** 当前流程节点编码（如 leaderAudit / managerAudit） */
    @TableField("node_code")
    private String nodeCode;

    /** 当前流程节点名称（如 组长审批 / 经理审批） */
    @TableField("node_name")
    private String nodeName;

    /** 业务冗余的流程状态（0待提交 1审批中 2通过 8已完成 9已退回） */
    @TableField("flow_status")
    private String flowStatus;

    @TableField("create_time")
    private Timestamp createTime;

    @TableField("update_time")
    private Timestamp updateTime;
}
