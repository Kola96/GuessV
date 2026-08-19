package com.guessv;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.guessv.mapper")
@EnableScheduling
public class GuessVApplication {
    public static void main(String[] args) {
        SpringApplication.run(GuessVApplication.class, args);
    }
}
