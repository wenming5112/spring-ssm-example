package com.ssm.example.task;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @author ming
 * @version 1.0.0
 * @date 2022/4/5 18:28
 **/
@EnableScheduling
@Component
@Slf4j
public class ExampleTask {

    @Async("asyncTaskExecutor")
    @Scheduled(cron = "0/5 * * * * ? ")
    public void test1() {
        log.info("----定时任务1开始执行-----");
    }

    @Async
    @Scheduled(cron = "0/8 * * * * ? ")
    public void test2() {
        log.info("----定时任务2开始执行-----");
    }
}
