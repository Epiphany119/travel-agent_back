package com.travel.a2a;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * A2A运行时启动类
 * 
 * <p>注意：此模块不作为独立启动类使用，而是与travel-web一起启动。
 * 由于travel-web的@SpringBootApplication包含@ComponentScan("com.travel")，
 * 会自动扫描本模块的组件。</p>
 */
@SpringBootApplication
public class A2aRuntimeApplication {

    public static void main(String[] args) {
        SpringApplication.run(A2aRuntimeApplication.class, args);
    }
}
