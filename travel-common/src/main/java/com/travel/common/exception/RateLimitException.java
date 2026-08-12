package com.travel.common.exception;

import lombok.Getter;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 熔断异常 - 当API调用超过每日限额时抛出
 *
 * <p>PR#1 调整：包路径从 {@code com.travel.agent.common.exception} 改为
 * {@code com.travel.common.exception}，与 travel-common 模块的命名空间对齐。</p>
 */
@Getter
public class RateLimitException extends RuntimeException {

    private final String serviceName;
    private final int currentCount;
    private final int limit;
    private final LocalDateTime resetTime;

    public RateLimitException(String serviceName, int currentCount, int limit, LocalDateTime resetTime) {
        super(String.format("%s 服务今日调用次数已达上限（%d/%d），将于明天 %s 恢复",
                serviceName, currentCount, limit, resetTime.toLocalTime()));
        this.serviceName = serviceName;
        this.currentCount = currentCount;
        this.limit = limit;
        this.resetTime = resetTime;
    }

    /**
     * 获取剩余恢复时间（秒）
     */
    public long getSecondsUntilReset() {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(resetTime)) {
            return java.time.Duration.between(now, resetTime).getSeconds();
        }
        return 0;
    }

    /**
     * 获取友好的恢复时间提示
     */
    public String getResetHint() {
        long seconds = getSecondsUntilReset();
        if (seconds < 3600) {
            return String.format("约 %d 分钟后恢复", (seconds + 59) / 60);
        } else {
            long hours = (seconds + 3599) / 3600;
            return String.format("约 %d 小时后恢复", hours);
        }
    }
}
