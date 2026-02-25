package com.rita.community.controller;

import com.rita.community.mapper.UserMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * TestController
 * 作用：测试控制器，提供开发期调试接口。
 */
@RestController
public class TestController {

    private final UserMapper userMapper;

    public TestController(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @GetMapping("/test/db")
    public Long testDb() {
        return userMapper.selectCount(null);
    }
}

