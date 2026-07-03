package com.example.desensitize.serializer;

import com.example.desensitize.annotation.DataMask;
import com.example.desensitize.annotation.MaskType;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;

import java.io.IOException;

/**
 * ====================================================================
 * 【核心序列化器 —— DataMaskSerializer】
 *
 * 实现了 Jackson 的 ContextualSerializer 接口，能够感知字段上的
 * {@link DataMask} 注解，根据脱敏类型动态执行脱敏逻辑。
 *
 * ═════════════════════════════════════════════════════════════════════
 * 【工作原理】
 *
 * 1. Jackson 在构建序列化器树时，遇到 @JsonSerialize 注解会调用
 *    createContextual() 方法（因为实现了 ContextualSerializer 接口）
 *
 * 2. createContextual() 获取字段上的 @DataMask 注解，提取 type 值
 *    并设置到 serializer 实例的 type 字段中
 *
 * 3. 实际序列化时，serialize() 方法根据 type 选择对应的脱敏策略
 *    对字段值进行处理后写入 JSON
 *
 * 关键点：
 *   - 每个带 @DataMask 注解的字段都会有一个独立的 serializer 实例
 *   - createContextual() 在启动时只调用一次，无运行时反射开销
 * ====================================================================
 */
public class DataMaskSerializer extends JsonSerializer<String> implements ContextualSerializer {

    /** 脱敏类型，由 createContextual() 在启动时注入 */
    private MaskType type;

    /** 默认无参构造（Jackson 反射需要） */
    public DataMaskSerializer() {
    }

    /**
     * 带参构造，用于 createContextual() 创建带类型的实例。
     *
     * @param type 脱敏类型
     */
    public DataMaskSerializer(MaskType type) {
        this.type = type;
    }

    // ================================================================
    // ContextualSerializer 接口方法
    // 在 Jackson 构建序列化器树时被调用，获取注解信息
    // ================================================================

    /**
     * 上下文构建方法——Jackson 在初始化时调用此方法，
     * 我们可以从中获取字段上的 @DataMask 注解信息。
     *
     * @param prov    序列化器提供者
     * @param property 当前正在处理的 Bean 属性（包含字段注解）
     * @return 配置好脱敏类型的 DataMaskSerializer 实例
     * @throws JsonMappingException 如果发生映射错误
     */
    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property)
            throws JsonMappingException {

        // 如果 property 为 null（不是字段属性），返回默认序列化器
        if (property == null) {
            return prov.findValueSerializer(String.class, property);
        }

        // 获取字段上的 @DataMask 注解
        DataMask annotation = property.getAnnotation(DataMask.class);
        if (annotation == null) {
            // 没有 @DataMask 注解，使用 String 的默认序列化器
            return prov.findValueSerializer(String.class, property);
        }

        // 获取注解中的脱敏类型
        MaskType maskType = annotation.type();

        // 返回一个配置好类型的新实例
        return new DataMaskSerializer(maskType);
    }

    // ================================================================
    // 核心序列化方法——实际的脱敏逻辑在此执行
    // ================================================================

    /**
     * 序列化方法——将字段值脱敏后写入 JSON。
     *
     * @param value 原始字段值
     * @param gen   JSON 生成器
     * @param prov  序列化器提供者
     * @throws IOException 如果写入发生 I/O 错误
     */
    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider prov)
            throws IOException {

        // 处理 null 值
        if (value == null) {
            gen.writeNull();
            return;
        }

        // 空字符串直接输出
        if (value.isEmpty()) {
            gen.writeString("");
            return;
        }

        // 根据脱敏类型执行脱敏
        String masked = applyMask(value, this.type);
        gen.writeString(masked);
    }

    // ================================================================
    // 脱敏策略分发 —— 根据 MaskType 选择对应的脱敏规则
    // ================================================================

    /**
     * 根据脱敏类型对字符串值进行脱敏处理。
     *
     * @param value 原始字符串
     * @param type  脱敏类型
     * @return 脱敏后的字符串
     */
    private String applyMask(String value, MaskType type) {
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

    // ================================================================
    // 具体脱敏规则实现
    // ================================================================

    /**
     * 手机号脱敏
     * 规则：保留前 3 位 + 后 4 位，中间用 **** 填充
     * 示例：13812345678 → 138****5678
     *
     * 安全处理：
     * - 长度不足 7 位时，保留前 3 位后全部脱敏
     * - 长度不足 3 位时，全部脱敏
     */
    private String maskPhone(String phone) {
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
     * 规则：
     *   - 2 字姓名：保留第 1 位，后 1 位脱敏 → 张*
     *   - 3 字姓名：保留第 1 位，后 2 位脱敏 → 李**
     *   - 4 字姓名（复姓）：保留前 2 位，后 2 位脱敏 → 欧阳**
     *   - 单名：保留第 1 位，后全脱敏 → 王*
     *   - 1 个字：不脱敏
     */
    private String maskName(String name) {
        if (name.length() <= 1) {
            return name;
        }

        if (name.length() == 2) {
            // "张三" → "张*"
            return name.charAt(0) + "*";
        }

        if (name.length() == 3) {
            // "李小明" → "李**"
            return name.charAt(0) + "**";
        }

        // 4 字及以上 (复姓如"欧阳修"或"司马相如")
        // 保留前 2 位 + 剩余长度个 *
        int keepLen = 2;
        StringBuilder sb = new StringBuilder();
        sb.append(name, 0, keepLen);
        for (int i = keepLen; i < name.length(); i++) {
            sb.append('*');
        }
        return sb.toString();
    }

    /**
     * 身份证号脱敏
     * 规则：保留前 6 位 + 后 4 位，中间用 8 个 * 填充
     * 示例：110101199001011234 → 110101****1234
     *
     * 安全处理：长度不足 10 位时，前 6 位保留，后面全脱敏
     */
    private String maskIdCard(String idCard) {
        if (idCard.length() <= 6) {
            // 太短无法保留前 6 位，全部脱敏
            return repeat('*', idCard.length());
        }

        if (idCard.length() <= 10) {
            // 能保留前 6 位，但不足以后 4 位
            return idCard.substring(0, 6) + repeat('*', idCard.length() - 6);
        }

        // 正常 18 位身份证
        return idCard.substring(0, 6) + repeat('*', idCard.length() - 10) + idCard.substring(idCard.length() - 4);
    }

    /**
     * 邮箱脱敏
     * 规则：保留第一个字符 + @ 及之后部分，中间填充 *
     * 示例：zhangsan@example.com → z******@example.com
     *
     * 安全处理：
     * - 没有 @ 符号则不脱敏（视为不合法邮箱）
     * - @ 前面只有一个字符，则不脱敏（太短没有意义）
     */
    private String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            // 没有 @ 或 @ 在第一个字符位置，不脱敏
            return email;
        }

        // 保留第一个字符 + **** + @ 及以后部分
        char firstChar = email.charAt(0);
        StringBuilder sb = new StringBuilder();
        sb.append(firstChar);
        // 中间填充 *，长度为 @ 前的字符数 - 1
        for (int i = 1; i < atIndex; i++) {
            sb.append('*');
        }
        // 追加 @ 及后面的域名部分
        sb.append(email.substring(atIndex));
        return sb.toString();
    }

    /**
     * 地址脱敏
     * 规则：保留前 6 个字符，其余用 * 填充
     * 示例：北京市朝阳区建国路88号 → 北京市朝阳区****
     *
     * 安全处理：
     * - 长度不足 6 位则保留全部
     * - 长度 6-9 位，保留 6 位后全部脱敏
     */
    private String maskAddress(String address) {
        if (address.length() <= 6) {
            return address;
        }

        return address.substring(0, 6) + repeat('*', address.length() - 6);
    }

    /**
     * 自定义脱敏
     * 规则：保留前 retainStart 个 + 后 retainEnd 个，中间用 * 填充
     * 此实现使用默认值（前 3 后 4），可通过 DesensitizeProperties 配置
     */
    private String maskCustom(String value) {
        // 默认使用前 3 后 4 的规则
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
     * 工具方法：重复字符 c 共 count 次
     *
     * @param c     要重复的字符
     * @param count 重复次数
     * @return 由 count 个 c 组成的字符串
     */
    private String repeat(char c, int count) {
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append(c);
        }
        return sb.toString();
    }
}