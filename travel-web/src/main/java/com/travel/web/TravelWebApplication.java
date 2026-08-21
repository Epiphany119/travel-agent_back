package com.travel.web;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * 旅行Agent Web启动类（唯一入口）
 *
 * <p>PR#1 调整：移除调试期打印 BeanDefinition 的逻辑；{@code @MapperScan}
 * 从 {@code "com.travel"} 收窄到
 * {@code "com.travel.module.**.infra.persistence"}，用 {@code **} 递归匹配
 * {@code agent.biz.infra.persistence} / {@code itinerary.biz.infra.persistence}
 * 等多层级包路径，避免误扫到 {@code common} 等非持久层包。</p>
 *
 * <p>一键启动：主服务（8080）+ 4 个 MCP Server（8081-8084）全部在同一次
 * {@code SpringApplication.run()} 中完成，无需手动分别启动。
 * MCP Server 的组件（{@code @Service}/{@code @Controller} 等）通过包排除规则
 * 隔离在主上下文之外，各自通过 {@code McpServerLauncher} 独立启动。</p>
 */
@SpringBootApplication(exclude = {
        org.springframework.ai.autoconfigure.openai.OpenAiAutoConfiguration.class
})
@ComponentScan(
        basePackages = "com.travel",
        // 排除所有 MCP Server 包，防止主上下文错误扫描到它们的 @Service/@Controller
        // McpServerLauncher 会单独启动这些子上下文
        excludeFilters = {
                @ComponentScan.Filter(
                        type = FilterType.REGEX,
                        pattern = "com\\.travel\\.mcp\\.server\\.weather\\..*"
                ),
                @ComponentScan.Filter(
                        type = FilterType.REGEX,
                        pattern = "com\\.travel\\.mcp\\.server\\.poi\\..*"
                ),
                @ComponentScan.Filter(
                        type = FilterType.REGEX,
                        pattern = "com\\.travel\\.mcp\\.server\\.meal\\..*"
                ),
                @ComponentScan.Filter(
                        type = FilterType.REGEX,
                        pattern = "com\\.travel\\.mcp\\.server\\.budget\\..*"
                )
        }
)
@MapperScan(basePackages = {"com.travel.module.**.infra.persistence", "com.travel.agent.persistence"})
public class TravelWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(TravelWebApplication.class, args);
    }
}