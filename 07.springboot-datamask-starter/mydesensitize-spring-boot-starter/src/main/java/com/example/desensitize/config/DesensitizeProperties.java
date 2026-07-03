package com.example.desensitize.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * ====================================================================
 * 【配置属性类 —— DesensitizeProperties】
 *
 * 将 application.properties / application.yml 中以 "mydesensitize" 前缀
 * 开头的配置项自动绑定到本类的字段上。
 *
 * 典型用法（在消费方 application.properties 中）：
 *   mydesensitize.mask-char=*
 *   mydesensitize.phone-front-keep=3
 *   mydesensitize.phone-back-keep=4
 *   mydesensitize.name-surname-keep=1
 *
 * 设计理念：
 *   让使用者能通过配置文件自定义脱敏行为，无需修改代码。
 * ====================================================================
 */
@Data
@ConfigurationProperties(prefix = "mydesensitize")
public class DesensitizeProperties {

    /**
     * 脱敏符号，默认为 '*'
     */
    private char maskChar = '*';

    /**
     * 手机号脱敏时保留的前几位，默认为 3
     * 示例：13812345678 → 138****5678（保留前3位 + 后4位）
     */
    private int phoneFrontKeep = 3;

    /**
     * 手机号脱敏时保留的后几位，默认为 4
     */
    private int phoneBackKeep = 4;

    /**
     * 姓名脱敏时保留的姓的位数，默认为 1
     * 示例：张三 → 张*（保留第1位）
     *       欧阳修 → 欧阳*（保留前2位，复姓场景）
     */
    private int nameSurnameKeep = 1;

    /**
     * 身份证号脱敏时保留的前几位，默认为 6
     */
    private int idCardFrontKeep = 6;

    /**
     * 身份证号脱敏时保留的后几位，默认为 4
     */
    private int idCardBackKeep = 4;

    /**
     * 地址脱敏时保留的前几位，默认为 6
     */
    private int addressFrontKeep = 6;
}
