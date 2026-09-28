package com.example.workflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.workflow.entity.Leave;
import com.example.workflow.mapper.LeaveMapper;

import lombok.extern.slf4j.Slf4j;

import org.dromara.warm.flow.core.FlowFactory;
import org.dromara.warm.flow.core.dto.FlowParams;
import org.dromara.warm.flow.core.entity.Definition;
import org.dromara.warm.flow.core.entity.HisTask;
import org.dromara.warm.flow.core.entity.Instance;
import org.dromara.warm.flow.core.entity.Task;
import org.dromara.warm.flow.core.service.DefService;
import org.dromara.warm.flow.core.service.HisTaskService;
import org.dromara.warm.flow.core.service.InsService;
import org.dromara.warm.flow.core.service.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 请假业务服务 —— 演示「业务系统」如何调用「Warm-Flow 引擎」完成一次审批流程。
 *
 * 三个核心动作对应的引擎 API：
 *   1. 发起请假  -> insService.start(businessId, flowParams)   启动流程实例
 *   2. 审批通过  -> taskService.skip(taskId, skipParams)       任务流转到下一节点
 *   3. 审批退回  -> taskService.skip(taskId, skipParams)       退回上一节点（skipType=REJECT）
 *
 * 业务表 t_leave 与引擎表通过 instance_id 关联，业务侧冗余 node_code/node_name/flow_status
 * 便于直接查询当前进度（见实体类注释）。
 */
@Service
@Slf4j 
public class LeaveService {

    /** 流程编码，与 leaveFlow.xml 根节点 flowCode 一致 */
    private static final String FLOW_CODE = "leaveFlow";

    @Resource
    private LeaveMapper leaveMapper;

    @Resource
    private DefService defService;

    @Resource
    private InsService insService;

    @Resource
    private TaskService taskService;

    @Resource
    private HisTaskService hisTaskService;

    /**
     * 初始化流程定义：读取 db/leaveFlow.xml，导入并发布「请假审批流程」。
     *
     * 刻意做成显式方法（由 /leave/init 接口手动调用），
     * 让学习者能亲眼看到 warm-flow 的两个核心 API：
     *   1. defService.importXml(InputStream)  -> 解析 XML，把节点/跳转写入引擎表
     *   2. defService.publish(definitionId)   -> 发布，只有「已发布」的流程才能启动实例
     *
     * 幂等：流程已存在则跳过导入，已发布则不再重复发布。
     */
    public Map<String, Object> initFlowDefinition() {
        Map<String, Object> result = new HashMap<>();

        // 1. 按流程编码查是否已导入过
        List<Definition> existList = defService.queryByCodeList(Collections.singletonList(FLOW_CODE));
        if (existList != null && !existList.isEmpty()) {
            Definition exist = existList.get(0);
            result.put("definitionId", exist.getId());
            result.put("isPublish", exist.getIsPublish());
            if (exist.getIsPublish() != null && exist.getIsPublish() == 2) {
                result.put("step1", "检查：流程 [leaveFlow] 已存在（definitionId=" + exist.getId() + "）");
                result.put("step2", "检查：流程已发布，无需重复初始化");
                result.put("message", "流程定义已存在且已发布，无需重复初始化");
                return result;
            }
            // 存在但未发布 → 补发布
            defService.publish(exist.getId());
            result.put("step1", "检查：流程 [leaveFlow] 已存在但未发布（definitionId=" + exist.getId() + "）");
            result.put("step2", "发布：defService.publish(" + exist.getId() + ") 发布成功");
            result.put("message", "流程定义已存在，已重新发布");
            return result;
        }

        // 2. 不存在 → 导入
        try (InputStream is = new ClassPathResource("db/leaveFlow.xml").getInputStream()) {
            Definition definition = defService.importXml(is);
            result.put("definitionId", definition.getId());
            result.put("step1", "导入：defService.importXml(db/leaveFlow.xml) 成功，生成 definitionId=" + definition.getId());

            // 3. 发布
            defService.publish(definition.getId());
            result.put("step2", "发布：defService.publish(" + definition.getId() + ") 成功，可通过 /leave/start 发起请假");
            result.put("message", "流程定义导入并发布成功");
            return result;
        } catch (Exception e) {
            throw new IllegalStateException("导入流程定义失败", e);
        }
    }

    /**
     * 发起请假：先存业务单，再启动流程实例，回填实例 id 与当前节点。
     */
    public Leave startLeave(String applicant, Integer days, String reason) {
        // 1. 保存业务请假单（先落库，拿到业务主键）
        Leave leave = new Leave();
        leave.setApplicant(applicant);
        leave.setDays(days);
        leave.setReason(reason);
        leaveMapper.insert(leave);

        // 2. 组装流程变量（可传给网关条件、审批人表达式等，本案例演示传参）
        Map<String, Object> variable = new HashMap<>();
        variable.put("applicant", applicant);
        variable.put("days", days);
        variable.put("reason", reason);

        // 3. 启动流程实例
        //    start(businessId, flowParams)：第一个参数是业务 id（这里用请假单主键）
        FlowParams flowParams = FlowParams.build()
                .flowCode(FLOW_CODE)                 // 流程编码（必须与定义一致）
                .handler(applicant)                  // 发起人/当前办理人
                .variable(variable);                 // 流程变量
        Instance instance = insService.start(String.valueOf(leave.getId()), flowParams);
        log.info("发起请假成功：leaveId={}, instanceId={}, 当前节点={}({})",
                leave.getId(), instance.getId(), instance.getNodeName(), instance.getNodeCode());

        // 4. 回填流程关联字段到业务表
        leave.setInstanceId(instance.getId());
        leave.setNodeCode(instance.getNodeCode());
        leave.setNodeName(instance.getNodeName());
        leave.setFlowStatus(instance.getFlowStatus());
        leaveMapper.updateById(leave);
        return leave;
    }

    /**
     * 审批：通过(PASS) 或 退回(REJECT)。
     *
     * @param leaveId        请假单 id
     * @param pass            true 通过 / false 退回
     * @param operatorId     当前办理人（审批人）
     * @param permissionFlag 办理人权限标识（对应节点 permission_flag，如 role:leader / role:manager）
     * @param message        审批意见
     */
    public Leave approve(Long leaveId, boolean pass, String operatorId, String permissionFlag, String message) {
        Leave leave = getById(leaveId);

        // 1. 根据 instanceId 查出当前待办任务（一个实例同一时刻通常只有一条待办）
        List<Task> tasks = listTodoTask(leave.getInstanceId());
        if (tasks.isEmpty()) {
            throw new IllegalStateException("该请假单没有待办任务，可能已结束");
        }
        Task task = tasks.get(0);

        // 2. 组装流转参数：PASS 通过 / REJECT 退回
        FlowParams skipParams = FlowParams.build()
                .skipType(pass ? "PASS" : "REJECT")     // 跳转类型（必传）
                .handler(operatorId)                    // 当前办理人
                .permissionFlag(Collections.singletonList(permissionFlag)) // 办理人权限，用于与节点权限校验
                .message(message);                      // 审批意见
        Instance instance = taskService.skip(task.getId(), skipParams);
        log.info("{} 审批：leaveId={}, {} -> 节点 {}",
                pass ? "通过" : "退回", leaveId, pass ? "PASS" : "REJECT", instance.getNodeName());

        // 3. 同步业务表节点与状态
        leave.setNodeCode(instance.getNodeCode());
        leave.setNodeName(instance.getNodeName());
        leave.setFlowStatus(instance.getFlowStatus());
        leaveMapper.updateById(leave);
        return leave;
    }

    /**
     * 查询某条请假单当前有哪些待办任务。
     */
    public List<Task> listTodoTask(Long instanceId) {
        Task query = FlowFactory.newTask();
        query.setInstanceId(instanceId);
        return taskService.list(query);
    }

    /**
     * 查询某条请假单的审批历史（每次流转都会在 flow_his_task 留一条记录）。
     */
    public List<HisTask> listHisTask(Long instanceId) {
        HisTask query = FlowFactory.newHisTask();
        query.setInstanceId(instanceId);
        return hisTaskService.list(query);
    }

    public Leave getById(Long id) {
        return leaveMapper.selectById(id);
    }

    public List<Leave> listAll() {
        return leaveMapper.selectList(new LambdaQueryWrapper<Leave>().orderByDesc(Leave::getId));
    }
}
