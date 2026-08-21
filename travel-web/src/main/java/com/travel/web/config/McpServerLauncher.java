package com.travel.web.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * MCP Server 嵌入式启动器。
 *
 * <p>在主服务（8080）启动后，自动在后台线程中启动 4 个 MCP Server：
 * <ul>
 *   <li>Weather  Server → 8081</li>
 *   <li>POI      Server → 8082</li>
 *   <li>Meal     Server → 8083</li>
 *   <li>Budget   Server → 8084</li>
 * </ul>
 *
 * <p>每个 Server 运行在独立线程中，共享主进程的类路径。
 * API Key 从主服务的 application.yml 中通过 travel.* 属性注入。
 */
@Slf4j
@Configuration
public class McpServerLauncher implements WebMvcConfigurer {

    @Value("${travel.weather.api-key:}")
    private String weatherApiKey;

    @Value("${travel.amap.api-key:}")
    private String amapApiKey;

    private volatile boolean launched = false;

    @EventListener
    @Order(0)
    public void onApplicationReady(ApplicationReadyEvent event) {
        if (launched) return;
        launched = true;

        log.info("========================================");
        log.info("  正在启动 MCP Server 集群 (同一 JVM)");
        log.info("========================================");

        sleep(500);

        startInNewThread("weather-mcp", 8081, buildWeatherProps());
        sleep(300);
        startInNewThread("poi-mcp", 8082, buildPoiProps());
        sleep(300);
        startInNewThread("meal-mcp", 8083, buildMealProps());
        sleep(300);
        startInNewThread("budget-mcp", 8084, buildBudgetProps());

        log.info("  MCP Server 启动命令已全部发出，"
                + "各服务将在后台初始化（首次请求时真正就绪）");
        log.info("========================================");
    }

    private void startInNewThread(String name, int port, Map<String, Object> props) {
        Thread t = new Thread(() -> {
            try {
                launchMcpServer(name, port, props);
            } catch (Exception e) {
                log.error("  [{}] 启动失败: {}", name, e.getMessage(), e);
            }
        });
        t.setName(name);
        t.setDaemon(true);
        t.start();
    }

    private void launchMcpServer(String name, int port, Map<String, Object> extraProps) throws Exception {
        log.info("  [{}] 正在启动，端口={}", name, port);

        ConfigurableEnvironment env = new org.springframework.core.env.StandardEnvironment();
        MutablePropertySources propSources = env.getPropertySources();

        Map<String, Object> allProps = new HashMap<>(extraProps);
        allProps.put("server.port", port);
        allProps.put("spring.main.web-application-type", "reactive");
        // 同 JVM 多应用共享 JMX MBeanServer，必须禁用子应用的 JMX
        allProps.put("spring.jmx.enabled", "false");
        allProps.put("spring.application.admin.enabled", "false");
        propSources.addFirst(new MapPropertySource("mcpServerProps", allProps));

        Map<String, Object> travelProps = new HashMap<>();
        travelProps.put("travel.weather.api-key", weatherApiKey);
        travelProps.put("travel.amap.api-key", amapApiKey);
        travelProps.put("travel.weather.enabled", "true");
        travelProps.put("travel.amap.enabled", "true");
        propSources.addLast(new MapPropertySource("travelProps", travelProps));

        // 先构造 SpringApplication，再先把 sources 放进去，最后设置 environment
        // 这是 Spring Boot 3.x 的正确顺序：sources 必须在 environment 之前
        SpringApplication app = new SpringApplication();
        app.setBannerMode(Banner.Mode.OFF);

        if ("weather-mcp".equals(name)) {
            app.setSources(Set.of(
                    "com.travel.mcp.server.weather.WeatherMcpServerApplication"));
        } else if ("poi-mcp".equals(name)) {
            app.setSources(Set.of(
                    "com.travel.mcp.server.poi.PoiMcpServerApplication"));
        } else if ("meal-mcp".equals(name)) {
            app.setSources(Set.of(
                    "com.travel.mcp.server.meal.MealMcpServerApplication"));
        } else if ("budget-mcp".equals(name)) {
            app.setSources(Set.of(
                    "com.travel.mcp.server.budget.BudgetMcpServerApplication"));
        } else {
            throw new IllegalStateException("Unknown MCP server: " + name);
        }

        app.setEnvironment(env);

        ConfigurableApplicationContext ctx = app.run(new String[0]);

        log.info("  [{}] 启动完成，端口={}", name, port);

        Thread holder = Thread.currentThread();
        holder.setName(name + "-holder");
        synchronized (holder) {
            holder.wait();
        }
    }

    private Map<String, Object> buildWeatherProps() {
        Map<String, Object> p = new HashMap<>();
        p.put("spring.application.name", "travel-mcp-server-weather");
        p.put("travel.weather.amap-key", weatherApiKey);
        return p;
    }

    private Map<String, Object> buildPoiProps() {
        Map<String, Object> p = new HashMap<>();
        p.put("spring.application.name", "travel-mcp-server-poi");
        p.put("travel.poi.amap-key", amapApiKey);
        return p;
    }

    private Map<String, Object> buildMealProps() {
        Map<String, Object> p = new HashMap<>();
        p.put("spring.application.name", "travel-mcp-server-meal");
        p.put("travel.meal.poi-server-url", "http://localhost:8082");
        return p;
    }

    private Map<String, Object> buildBudgetProps() {
        Map<String, Object> p = new HashMap<>();
        p.put("spring.application.name", "travel-mcp-server-budget");
        return p;
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}
