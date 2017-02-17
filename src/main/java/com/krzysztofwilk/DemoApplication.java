package com.krzysztofwilk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@SuppressWarnings("squid:S1118")
public class DemoApplication {

    /**
     * Spring Boot entry method.
     * @param args ignored arguments
     */
    @SuppressWarnings("squid:S2095")
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
