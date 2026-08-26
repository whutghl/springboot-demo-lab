package com.example.spdemo.service;

import com.example.spdemo.entity.User;
import com.example.spdemo.mapper.UserMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 用户服务，对 UserMapper 的薄封装，方便教学演示。
 *
 * 注入方式：@Resource 字段注入（按名称查找 Bean，找不到再按类型）。
 * 本示例刻意演示这种写法，便于对比：
 *   - @Resource（javax 注解，按名称优先） / @Autowired（Spring 注解，按类型优先）
 *   - 字段注入代码更短，但依赖不可 final、缺失时启动不报错（调用时才 NPE），
 *     单元测试也依赖 Spring 容器。生产项目更推荐构造器注入（见 guide 或注释）。
 */
@Service
public class UserService {

    @Resource
    private UserMapper userMapper;

    /**
     * 查询全部用户。
     * 数据来自 Flyway V3 脚本插入的演示数据 + 后续通过接口新增的数据。
     */
    public List<User> listAll() {
        return userMapper.selectList(null);
    }

    /**
     * 新增用户，对应「运行时通过接口写入新数据」。
     * 注意：这只是业务数据写入，与表结构变更无关——
     * 结构变更永远走「新迁移脚本」，二者要严格分开，这是 Flyway 的核心心智模型。
     */
    public User add(String name, String email, String phone) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPhone(phone);
        userMapper.insert(user); // 插入成功后，user.id 会被自动回填（IdType.AUTO）
        return user;
    }
}
