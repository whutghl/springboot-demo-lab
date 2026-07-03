package com.example.consumer.controller;

import com.example.consumer.entity.UserVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/**
 * ====================================================================
 * 【用户控制器 —— UserController】
 *
 * 演示 @DataMask 注解的自动脱敏效果。
 *
 * 返回 UserVO 对象时，Jackson 会自动对带有 @DataMask 注解的字段
 * 进行脱敏处理，无需手动调用任何脱敏方法。
 *
 * 访问：
 *   GET /user          → 返回单个用户（所有敏感字段已脱敏）
 *   GET /users         → 返回用户列表（所有敏感字段已脱敏）
 * ====================================================================
 */
@RestController
public class UserController {

    /**
     * GET /user
     * 返回单个用户的脱敏信息
     *
     * @return UserVO 对象，敏感字段自动脱敏
     */
    @GetMapping("/user")
    public UserVO getUser() {
        return new UserVO(
                1L,
                "张三",
                "13812345678",
                "110101199001011234",
                "zhangsan@example.com",
                "北京市朝阳区建国路88号"
        );
    }

    /**
     * GET /users
     * 返回多个用户的脱敏信息列表
     *
     * @return List<UserVO> 用户列表，敏感字段自动脱敏
     */
    @GetMapping("/users")
    public List<UserVO> getUsers() {
        return Arrays.asList(
                new UserVO(1L, "张三", "13812345678",
                        "110101199001011234", "zhangsan@example.com",
                        "北京市朝阳区建国路88号"),
                new UserVO(2L, "李小明", "13998765432",
                        "320105199503022345", "lixiaoming@test.com",
                        "上海市浦东新区张江高科技园区"),
                new UserVO(3L, "欧阳修", "18600001111",
                        "440305198807123456", "ouyangxiu@example.com",
                        "广东省深圳市南山区科技园南路")
        );
    }
}