package com.example.spdemo.entity;

import lombok.Data;

/**
 * 演示用 POJO：用于展示对象引用在多线程下的可见性与竞态问题
 */
@Data
public class User {

    private String name;
    private int age;

    public User() {
    }

    public User(String name, int age) {
        this.name = name;
        this.age = age;
    }
}
