package com.olyv;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.integration.config.EnableIntegration;

@SpringBootApplication
@EnableIntegration
public class App {

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }
}
