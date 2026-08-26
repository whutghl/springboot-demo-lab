package com.example.spdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.spdemo.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper。
 *
 * 继承 BaseMapper<User> 后，MyBatis-Plus 自动为我们提供单表 CRUD：
 *   selectList(...)  → 条件查询（空条件即查全部）
 *   insert(...)      → 插入
 *   selectById / updateById / deleteById ... 还有很多，见 MyBatis-Plus 文档
 *
 * @Mapper 注解让 Spring 扫描到这个接口并生成代理实现（等价做法：
 * 在启动类上加 @MapperScan("com.example.spdemo.mapper") 批量扫描，二选一即可）。
 *
 * 表结构由 Flyway 保证存在，这里只负责读写，职责单一。
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
