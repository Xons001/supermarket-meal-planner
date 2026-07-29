package com.sean.supermarketmealplanner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SupermarketMealPlannerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SupermarketMealPlannerApplication.class, args);
    }
}
