package com.rita.community.dto;

/**
 * RegisterReq
 * 作用：请求参数模型，承接前端提交字段并配合注解做基础校验。
 */
public class RegisterReq {
    private String phone;
    private String password;
    private String nickname;

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

    public String getNickname() {
        return nickname;
    }
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
}

