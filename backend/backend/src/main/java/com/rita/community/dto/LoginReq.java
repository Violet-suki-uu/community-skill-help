package com.rita.community.dto;

/**
 * LoginReq
 * 作用：请求参数模型，承接前端提交字段并配合注解做基础校验。
 */
public class LoginReq {
    private String phone;
    private String password;

    public String getPhone() {
        return phone;
    }
    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
}

