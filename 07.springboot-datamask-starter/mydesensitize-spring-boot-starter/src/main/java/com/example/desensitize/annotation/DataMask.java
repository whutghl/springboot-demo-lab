package com.example.desensitize.annotation;

import com.example.desensitize.serializer.DataMaskSerializer;
import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ====================================================================
 * 【数据脱敏注解 —— @DataMask】
 *
 * 在实体类的 String 类型字段上标注此注解，
 * 当 Jackson 序列化该对象为 JSON 时，自动根据指定的脱敏类型
 * 对字段值进行脱敏处理。
 *
 * ═════════════════════════════════════════════════════════════════════
 * 【使用示例】
 *
 * public class UserVO {
 *     @DataMask(type = MaskType.NAME)
 *     private String name;
 *
 *     @DataMask(type = MaskType.PHONE)
 *     private String phone;
 * }
 *
 * 序列化结果：
 *   {
 *       "name": "张*",
 *       "phone": "138****5678"
 *   }
 * ═════════════════════════════════════════════════════════════════════
 *
 * 【核心原理 —— Jackson 的 ContextualSerializer】
 *
 * 1. @JsonSerialize(using = DataMaskSerializer.class) 声明使用自定义序列化器
 * 2. DataMaskSerializer 实现了 ContextualSerializer 接口
 * 3. 在序列化时，Jackson 会调用 createContextual() 方法，
 *    该方法能获取到字段上的 @DataMask 注解信息
 * 4. 根据注解中的 type() 值，选择对应的脱敏策略
 * 5. 最终输出的 JSON 值即为脱敏后的字符串
 *
 * 这种设计使得"脱敏"完全透明——业务代码只需标注注解，
 * 无需调用任何脱敏方法，序列化过程自动完成。
 * ====================================================================
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@JacksonAnnotationsInside
@JsonSerialize(using = DataMaskSerializer.class)
public @interface DataMask {

    /**
     * 脱敏类型，指定要对字段应用哪种脱敏规则。
     *
     * @return MaskType 枚举值，默认为 PHONE
     */
    MaskType type() default MaskType.PHONE;
}