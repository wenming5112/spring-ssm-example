package com.ssm.example.task.conf;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author ming
 * @version 1.0.0
 * @date 2022/4/5 18:44
 **/
@Configuration
@EnableAsync
public class TaskExecutorConfig {

    /**
     * 自定义异步线程池
     */

    @Bean(name = "asyncTaskExecutor")
    public AsyncTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("asyncTaskExecutor");
        executor.setMaxPoolSize(10);
        executor.setCorePoolSize(3);
        executor.setQueueCapacity(100);
        // 设置自定义拒绝策略
        // executor.setRejectedExecutionHandler(new RejectedExecutionHandler() {
        //     @Override
        //     public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        //         // .....
        //         // 也可以使用默认的拒绝策略，在spring中指定策略名称
        //     }
        // });
        // 使用默认的拒绝策略，有四种
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        //等待所有任务调度完成在关闭线程池，保证所有的任务被正确处理
        executor.setWaitForTasksToCompleteOnShutdown(true);
        //线程池关闭时等待其他任务的时间，不能无限等待，确保应用最后能被关闭。而不是无限期阻塞
        executor.setAwaitTerminationSeconds(60);
        //线程池初始化
        executor.initialize();
        return executor;
    }

}