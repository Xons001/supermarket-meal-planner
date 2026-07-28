package com.sean.supermarketmealplanner.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI applicationOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Supermarket Meal Planner API")
                .version("v1")
                .description("Independent demo API. Prices and availability are estimates."));
    }
}
