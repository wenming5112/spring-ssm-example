package com.ssm.example.service.impl;

import com.ssm.example.sdk.ExampleService;
import org.springframework.stereotype.Service;

/**
 * @author ming
 * @version 1.0.0
 * @date 2022/4/5 14:04
 **/
@Service("exampleService")
public class ExampleServiceImpl implements ExampleService {

    @Override
    public String example() {
        return "hello";
    }
}
