package com.travel.agent.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 异步执行配置
 *
 * <p>为 @Async 注解提供异步执行器，用于 HostAgentService 的 SSE 流异步处理。</p>
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 异步任务执行器
     * 用于 SSE 流异步输出，避免阻塞 HTTP 线程
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("Async-");
        executor.setRejectedExecutionHandler((r, e) -> 
            log.warn("异步任务被拒绝，执行器已满载"));
        executor.initialize();
        return executor;
    }
}
