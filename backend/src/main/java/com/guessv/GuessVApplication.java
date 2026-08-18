package com.guessv;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.guessv.mapper")
public class GuessVApplication {
    public static void main(String[] args) {
        SpringApplication.run(GuessVApplication.class, args);
    }
}
