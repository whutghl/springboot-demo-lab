package com.example.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.workflow.entity.Leave;
import org.apache.ibatis.annotations.Mapper;

/**
 * 请假业务 Mapper，继承 BaseMapper<Leave> 获得单表 CRUD（selectList / insert / selectById 等）。
 *
 * 注意：这里扫描的是「业务表 t_leave」。warm-flow 引擎自己的 7 张表由 starter 内置的
 * Mapper（FlowDefinitionMapper / FlowInstanceMapper ...）管理，二者互不干扰。
 */
@Mapper
public interface LeaveMapper extends BaseMapper<Leave> {
}
