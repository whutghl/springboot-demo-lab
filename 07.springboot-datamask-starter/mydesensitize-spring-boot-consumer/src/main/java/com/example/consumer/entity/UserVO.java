package com.example.consumer.entity;

import com.example.desensitize.annotation.DataMask;
import com.example.desensitize.annotation.MaskType;

/**
 * ====================================================================
 * 【用户视图对象 —— UserVO】
 *
 * 演示 @DataMask 注解的使用方式。
 *
 * 每个 String 字段上标注 @DataMask(type = ...) 注解，
 * 当 Jackson 序列化此对象为 JSON 时，这些字段的值会自动被脱敏。
 *
 * 实际开发中，你只需要像这样在实体/VO 的字段上加上注解，
 * 后续返回 JSON 时就无需再手动处理脱敏逻辑。
 * ====================================================================
 */
public class UserVO {

    /** 用户ID（不脱敏） */
    private Long id;

    /** 姓名（脱敏：张三 → 张*） */
    @DataMask(type = MaskType.NAME)
    private String name;

    /** 手机号（脱敏：13812345678 → 138****5678） */
    @DataMask(type = MaskType.PHONE)
    private String phone;

    /** 身份证号（脱敏：110101199001011234 → 110101****1234） */
    @DataMask(type = MaskType.ID_CARD)
    private String idCard;

    /** 邮箱（脱敏：zhangsan@example.com → z******@example.com） */
    @DataMask(type = MaskType.EMAIL)
    private String email;

    /** 地址（脱敏：北京市朝阳区建国路88号 → 北京市朝阳区****） */
    @DataMask(type = MaskType.ADDRESS)
    private String address;

    // ================================================================
    // 构造方法
    // ================================================================

    public UserVO() {
    }

    public UserVO(Long id, String name, String phone, String idCard, String email, String address) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.idCard = idCard;
        this.email = email;
        this.address = address;
    }

    // ================================================================
    // Getter / Setter
    // ================================================================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getIdCard() {
        return idCard;
    }

    public void setIdCard(String idCard) {
        this.idCard = idCard;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}