package com.ssm.example.service.impl;

import com.ssm.example.common.utils.RedisCache;
import com.ssm.example.sdk.ExampleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author ming
 * @version 1.0.0
 * @date 2022/4/5 14:04
 **/

@Slf4j
@Service("exampleService")
public class ExampleServiceImpl implements ExampleService {

    @Resource
    private RedisCache redisCache;

    @Override
    public String example() {
        redisCache.setString("test", "123456");
        log.info("从redis获取：{}", redisCache.getString("test"));
        return "hello";
    }
}
