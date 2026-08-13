package com.travel.common.ratelimit;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.travel.common.exception.RateLimitException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 熔断器服务
 * 实现API调用限流，支持文件持久化和每日自动重置
 *
 * <p>PR#1 调整：包路径从 {@code com.travel.agent.common.service} 改为
 * {@code com.travel.common.ratelimit}；{@link com.travel.common.exception.RateLimitException}
 * 的引用路径同步更新。后续 PR#5 会按 Option A 进一步抽象成接口并实现可替换 executor。</p>
 */
@Slf4j
@Service
public class RateLimitService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Value("${travel.ratelimit.enabled:true}")
    private boolean enabled;

    @Value("${travel.ratelimit.weather.limit:800}")
    private int weatherLimit;

    @Value("${travel.ratelimit.amap.limit:4700}")
    private int amapLimit;

    @Value("${travel.ratelimit.data-path:data/rate-limit.json}")
    private String dataPath;

    private final ObjectMapper objectMapper;
    private final ReentrantLock fileLock = new ReentrantLock();
    private RateLimitData rateLimitData;
    private final Map<String, ServiceLimit> serviceLimits = new ConcurrentHashMap<>();

    public RateLimitService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @PostConstruct
    public void init() {
        // 初始化服务限额配置
        serviceLimits.put("weather", new ServiceLimit(weatherLimit, "天气服务"));
        serviceLimits.put("amap", new ServiceLimit(amapLimit, "高德地图服务"));

        // 加载持久化数据
        loadFromFile();

        // 检查是否需要重置（跨日期）
        checkAndResetIfNeeded();
    }

    @PreDestroy
    public void destroy() {
        // 保存到文件
        saveToFile();
    }

    /**
     * 尝试获取调用令牌，如果超限则抛出异常
     */
    public void tryAcquire(String serviceName) {
        if (!enabled) {
            return;
        }

        ServiceLimit limit = serviceLimits.get(serviceName);
        if (limit == null) {
            log.warn("未知的限流服务: {}", serviceName);
            return;
        }

        // 检查是否需要重置
        checkAndResetIfNeeded();

        // 检查是否超限
        int currentCount = rateLimitData.getServiceCounts().getOrDefault(serviceName, 0);
        if (currentCount >= limit.getLimit()) {
            LocalDateTime resetTime = getNextResetTime();
            throw new RateLimitException(limit.getDisplayName(), currentCount, limit.getLimit(), resetTime);
        }

        // 原子性增加计数
        rateLimitData.getServiceCounts().put(serviceName, currentCount + 1);

        // 异步保存到文件
        saveToFileAsync();

        log.debug("{} 调用成功，当前计数: {}/{}", limit.getDisplayName(), currentCount + 1, limit.getLimit());
    }

    /**
     * 获取服务的当前调用次数
     */
    public int getCurrentCount(String serviceName) {
        return rateLimitData.getServiceCounts().getOrDefault(serviceName, 0);
    }

    /**
     * 获取服务的每日限额
     */
    public int getLimit(String serviceName) {
        ServiceLimit limit = serviceLimits.get(serviceName);
        return limit != null ? limit.getLimit() : 0;
    }

    /**
     * 获取剩余可用次数
     */
    public int getRemaining(String serviceName) {
        return Math.max(0, getLimit(serviceName) - getCurrentCount(serviceName));
    }

    /**
     * 检查并重置计数（如果跨日期）
     */
    private void checkAndResetIfNeeded() {
        LocalDate today = LocalDate.now();
        LocalDate lastDate = rateLimitData.getCurrentDate();

        if (!today.equals(lastDate)) {
            log.info("检测到日期变更，重置熔断计数器: {} -> {}", lastDate, today);
            rateLimitData.setCurrentDate(today);
            rateLimitData.getServiceCounts().clear();
            saveToFile();
        }
    }

    /**
     * 获取下一个重置时间（明天零点）
     */
    private LocalDateTime getNextResetTime() {
        return LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.MIDNIGHT);
    }

    /**
     * 每天零点执行重置任务
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void scheduledReset() {
        log.info("定时任务：重置熔断计数器");
        rateLimitData.setCurrentDate(LocalDate.now());
        rateLimitData.getServiceCounts().clear();
        saveToFile();
    }

    /**
     * 从文件加载数据
     */
    private void loadFromFile() {
        fileLock.lock();
        try {
            Path path = Paths.get(dataPath);
            File file = path.toFile();
            if (file.exists()) {
                try {
                    rateLimitData = objectMapper.readValue(file, RateLimitData.class);
                    log.info("从文件加载熔断数据成功，当前日期: {}", rateLimitData.getCurrentDate());
                } catch (IOException e) {
                    log.warn("加载熔断数据失败，使用默认配置: {}", e.getMessage());
                    rateLimitData = new RateLimitData();
                }
            } else {
                rateLimitData = new RateLimitData();
                // 确保目录存在
                file.getParentFile().mkdirs();
            }
        } finally {
            fileLock.unlock();
        }
    }

    /**
     * 保存数据到文件
     */
    private void saveToFile() {
        fileLock.lock();
        try {
            Path path = Paths.get(dataPath);
            File file = path.toFile();
            file.getParentFile().mkdirs();
            objectMapper.writeValue(file, rateLimitData);
            log.debug("熔断数据已保存到文件");
        } catch (IOException e) {
            log.error("保存熔断数据失败: {}", e.getMessage());
        } finally {
            fileLock.unlock();
        }
    }

    /**
     * 异步保存到文件（延迟500ms，避免频繁IO）
     */
    private void saveToFileAsync() {
        new Thread(() -> {
            try {
                Thread.sleep(500);
                saveToFile();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    /**
     * 服务限额配置
     */
    @Data
    private static class ServiceLimit {
        private final int limit;
        private final String displayName;

        public ServiceLimit(int limit, String displayName) {
            this.limit = limit;
            this.displayName = displayName;
        }
    }

    /**
     * 熔断数据（用于JSON序列化）
     */
    @Data
    public static class RateLimitData {
        @JsonProperty("currentDate")
        private LocalDate currentDate = LocalDate.now();

        @JsonProperty("serviceCounts")
        private Map<String, Integer> serviceCounts = new ConcurrentHashMap<>();
    }
}
