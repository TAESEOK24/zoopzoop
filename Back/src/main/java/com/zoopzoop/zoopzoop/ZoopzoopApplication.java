package com.zoopzoop.zoopzoop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class ZoopzoopApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZoopzoopApplication.class, args);
    }

}
