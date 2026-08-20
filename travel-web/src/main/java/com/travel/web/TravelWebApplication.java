package com.travel.web;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * 旅行Agent Web启动类（唯一入口）
 *
 * <p>PR#1 调整：移除调试期打印 BeanDefinition 的逻辑；{@code @MapperScan}
 * 从 {@code "com.travel"} 收窄到
 * {@code "com.travel.module.**.infra.persistence"}，用 {@code **} 递归匹配
 * {@code agent.biz.infra.persistence} / {@code itinerary.biz.infra.persistence}
 * 等多层级包路径，避免误扫到 {@code common} 等非持久层包。</p>
 */
@SpringBootApplication(exclude = {
        org.springframework.ai.autoconfigure.openai.OpenAiAutoConfiguration.class
})
@ComponentScan(basePackages = "com.travel")
@MapperScan(basePackages = {"com.travel.module.**.infra.persistence", "com.travel.agent.persistence"})
public class TravelWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(TravelWebApplication.class, args);
    }
}