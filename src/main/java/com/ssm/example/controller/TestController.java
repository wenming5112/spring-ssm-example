package com.ssm.example.controller;

import com.ssm.example.sdk.ExampleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author ming
 * @version 1.0.0
 * @date 2022/4/5 14:02
 **/

@Controller
@RequestMapping("/test")
@Slf4j
public class TestController {

    @Autowired
    private ExampleService exampleService;

    @RequestMapping(value = "/example", method = RequestMethod.GET)
    @ResponseBody
    public String example() {
        log.info("进入测试controller");
        return exampleService.example();
    }
}
