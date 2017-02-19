package com.krzysztofwilk.spring.cloud.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {

    /**
     * Spring Boot entry method.
     * @param args ignored arguments
     */
    @SuppressWarnings("squid:S2095")
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
