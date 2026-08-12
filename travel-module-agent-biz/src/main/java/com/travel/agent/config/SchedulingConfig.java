package com.travel.agent.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 定时任务配置
 *
 * <p>启用 Spring Scheduling 以支持熔断器每日零点自动重置。
 * 实际定时任务（{@code @Scheduled(cron = "0 0 0 * * ?")}）位于
 * {@code com.travel.common.ratelimit.RateLimitService}，由本注解统一开启调度能力。</p>
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}