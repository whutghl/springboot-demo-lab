package com.example.spdemo.controller;

import com.example.spdemo.entity.User;
import com.example.spdemo.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户接口，用于验证 Flyway 建好的表能被正常读写。
 *
 *   GET /user/list        → 查询全部用户（含 V3 脚本插入的演示数据）
 *   GET /user/add?name=&email=&phone= → 新增用户（运行时的业务写入）
 */
@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/list")
    public Map<String, Object> list() {
        List<User> users = userService.listAll();

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("total", users.size());
        result.put("users", users);
        result.put("hint", "表中结构由 Flyway V1/V2 脚本创建，数据由 V3 脚本插入，这就是数据库即代码（database as code）");
        return result;
    }

    @GetMapping("/add")
    public Map<String, Object> add(@RequestParam String name,
                                   @RequestParam String email,
                                   @RequestParam(required = false) String phone) {
        User user = userService.add(name, email, phone);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "新增成功，id=" + user.getId());
        result.put("hint", "业务数据写入走接口；表结构变更则走新的迁移脚本 V4/V5...，两者互不干扰");
        return result;
    }
}
