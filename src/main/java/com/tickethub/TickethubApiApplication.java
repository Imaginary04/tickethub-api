package com.tickethub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TickethubApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(TickethubApiApplication.class, args);
    }

}