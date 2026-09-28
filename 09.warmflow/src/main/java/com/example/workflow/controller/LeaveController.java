package com.example.workflow.controller;

import com.example.workflow.entity.Leave;
import com.example.workflow.service.LeaveService;
import org.dromara.warm.flow.core.entity.HisTask;
import org.dromara.warm.flow.core.entity.Task;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 请假工作流接口，用于 curl 验证 Warm-Flow 引擎的完整流转。
 *
 *   GET /leave/init
 *        → 初始化流程定义（导入 db/leaveFlow.xml + 发布），首次使用前调用
 *   GET /leave/start?applicant=张三&days=3&reason=家中有事
 *        → 发起请假，流程走到「组长审批」节点
 *   GET /leave/list
 *        → 所有请假单（含当前节点/状态，来自业务表 t_leave）
 *   GET /leave/todo?instanceId=xxx
 *        → 查某个请假单当前待办任务（flow_task）
 *   GET /leave/approve?leaveId=1&pass=true&operatorId=组长&permissionFlag=role:leader&message=同意
 *        → 组长通过 → 流转到「经理审批」
 *   GET /leave/approve?leaveId=1&pass=true&operatorId=经理&permissionFlag=role:manager&message=同意
 *        → 经理通过 → 流程结束
 *   GET /leave/history?instanceId=xxx
 *        → 查审批历史（flow_his_task）
 */
@RestController
@RequestMapping("/leave")
public class LeaveController {

    @Resource
    private LeaveService leaveService;

    /**
     * 初始化流程定义：导入 db/leaveFlow.xml 并发布。
     * 显式手动调用（幂等），替代原来的启动自动导入，便于学习者理解。
     */
    @GetMapping("/init")
    public Map<String, Object> init() {
        Map<String, Object> data = leaveService.initFlowDefinition();
        return ok((String) data.get("message"), data);
    }

    /**
     * 发起请假，启动流程实例。
     */
    @GetMapping("/start")
    public Map<String, Object> start(@RequestParam String applicant,
                                     @RequestParam Integer days,
                                     @RequestParam(required = false) String reason) {
        Leave leave = leaveService.startLeave(applicant, days, reason);
        return ok("发起请假成功，流程已进入「" + leave.getNodeName() + "」节点", leave);
    }

    /**
     * 审批：pass=true 通过，pass=false 退回。
     */
    @GetMapping("/approve")
    public Map<String, Object> approve(@RequestParam Long leaveId,
                                       @RequestParam boolean pass,
                                       @RequestParam String operatorId,
                                       @RequestParam String permissionFlag,
                                       @RequestParam(required = false) String message) {
        Leave leave = leaveService.approve(leaveId, pass, operatorId, permissionFlag, message);
        return ok((pass ? "审批通过" : "审批退回") + "，当前节点「" + leave.getNodeName() + "」", leave);
    }

    /**
     * 查询某条请假单当前的待办任务。
     */
    @GetMapping("/todo")
    public Map<String, Object> todo(@RequestParam Long instanceId) {
        List<Task> tasks = leaveService.listTodoTask(instanceId);
        Map<String, Object> data = new HashMap<>();
        data.put("todoCount", tasks.size());
        data.put("tasks", tasks);
        return ok("查询待办成功", data);
    }

    /**
     * 查询某条请假单的审批历史。
     */
    @GetMapping("/history")
    public Map<String, Object> history(@RequestParam Long instanceId) {
        List<HisTask> hisTasks = leaveService.listHisTask(instanceId);
        Map<String, Object> data = new HashMap<>();
        data.put("historyCount", hisTasks.size());
        data.put("history", hisTasks);
        return ok("查询审批历史成功", data);
    }

    /**
     * 所有请假单。
     */
    @GetMapping("/list")
    public Map<String, Object> list() {
        List<Leave> leaves = leaveService.listAll();
        return ok("查询请假单列表成功", leaves);
    }

    private Map<String, Object> ok(String message, Object data) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", message);
        result.put("data", data);
        return result;
    }
}
