package com.example.desensitize.service;

import com.example.desensitize.annotation.MaskType;

/**
 * ====================================================================
 * 【脱敏工具类 —— DesensitizeUtil】
 *
 * 提供一组静态工具方法，用于在非序列化场景下手动脱敏。
 *
 * 适用场景：
 *   1. 日志输出中的敏感信息脱敏
 *   2. 不需要序列化为 JSON 的字段脱敏
 *   3. 在 Service 层或 Mapper 层直接脱敏
 *
 * 用法示例：
 *   String maskedPhone = DesensitizeUtil.desensitize("13812345678", MaskType.PHONE);
 *   // 结果：138****5678
 *
 * 注意：
 *   此工具类的脱敏逻辑与 DataMaskSerializer 保持一致，
 *   确保无论在注解方式还是代码调用方式下，结果都相同。
 * ====================================================================
 */
public class DesensitizeUtil {

    private DesensitizeUtil() {
        // 工具类，防止实例化
    }

    /**
     * 通用脱敏方法 —— 根据脱敏类型对输入字符串进行脱敏。
     *
     * @param value 原始字符串
     * @param type  脱敏类型
     * @return 脱敏后的字符串
     */
    public static String desensitize(String value, MaskType type) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        switch (type) {
            case PHONE:
                return maskPhone(value);
            case NAME:
                return maskName(value);
            case ID_CARD:
                return maskIdCard(value);
            case EMAIL:
                return maskEmail(value);
            case ADDRESS:
                return maskAddress(value);
            case CUSTOM:
                return maskCustom(value);
            default:
                return value;
        }
    }

    /**
     * 电话/手机号脱敏
     * 示例：13812345678 → 138****5678
     */
    public static String maskPhone(String phone) {
        if (phone.length() >= 7) {
            return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
        } else if (phone.length() >= 3) {
            return phone.substring(0, 3) + "****";
        } else {
            return "****";
        }
    }

    /**
     * 姓名脱敏
     * 示例：张三 → 张*，李小明 → 李**，欧阳修 → 欧阳*
     */
    public static String maskName(String name) {
        if (name.length() <= 1) {
            return name;
        }
        if (name.length() == 2) {
            return name.charAt(0) + "*";
        }
        if (name.length() == 3) {
            return name.charAt(0) + "**";
        }
        // 4 字及以上（复姓）
        return name.substring(0, 2) + repeat('*', name.length() - 2);
    }

    /**
     * 身份证号脱敏
     * 示例：110101199001011234 → 110101****1234
     */
    public static String maskIdCard(String idCard) {
        if (idCard.length() <= 6) {
            return repeat('*', idCard.length());
        }
        if (idCard.length() <= 10) {
            return idCard.substring(0, 6) + repeat('*', idCard.length() - 6);
        }
        return idCard.substring(0, 6)
                + repeat('*', idCard.length() - 10)
                + idCard.substring(idCard.length() - 4);
    }

    /**
     * 邮箱脱敏
     * 示例：zhangsan@example.com → z******@example.com
     */
    public static String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return email;
        }
        return email.charAt(0)
                + repeat('*', atIndex - 1)
                + email.substring(atIndex);
    }

    /**
     * 地址脱敏
     * 示例：北京市朝阳区建国路88号 → 北京市朝阳区****
     */
    public static String maskAddress(String address) {
        if (address.length() <= 6) {
            return address;
        }
        return address.substring(0, 6) + repeat('*', address.length() - 6);
    }

    /**
     * 自定义脱敏（默认前 3 后 4）
     */
    public static String maskCustom(String value) {
        int keepStart = 3;
        int keepEnd = 4;
        if (value.length() <= keepStart + keepEnd) {
            return value;
        }
        return value.substring(0, keepStart)
                + repeat('*', value.length() - keepStart - keepEnd)
                + value.substring(value.length() - keepEnd);
    }

    /**
     * 重复字符
     */
    private static String repeat(char c, int count) {
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append(c);
        }
        return sb.toString();
    }
}