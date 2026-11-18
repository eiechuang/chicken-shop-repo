package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ChickenShopApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChickenShopApplication.class, args);
    }
}