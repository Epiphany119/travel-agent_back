package com.travel.web;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * 旅行Agent Web启动类（唯一入口）
 */
@SpringBootApplication
@ComponentScan(basePackages = "com.travel")
@MapperScan(basePackages = "com.travel")
public class TravelWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(TravelWebApplication.class, args);
    }
}
