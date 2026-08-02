package com.sean.supermarketmealplanner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.sean.supermarketmealplanner.catalogsync.application.CatalogSyncProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@EnableConfigurationProperties(CatalogSyncProperties.class)
@ConfigurationPropertiesScan
public class SupermarketMealPlannerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SupermarketMealPlannerApplication.class, args);
    }
}
