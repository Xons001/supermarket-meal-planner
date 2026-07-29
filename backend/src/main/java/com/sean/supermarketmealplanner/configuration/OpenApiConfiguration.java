package com.sean.supermarketmealplanner.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI applicationOpenApi() {
        return new OpenAPI().components(new Components()
                        .addSecuritySchemes("accessCookie", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name("SMP_ACCESS")
                                .description("JWT de acceso HttpOnly; la SPA no puede leerlo"))
                        .addSecuritySchemes("csrfHeader", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-XSRF-TOKEN")
                                .description("Obtenido previamente mediante GET /api/v1/auth/csrf")))
                .info(new Info()
                .title("Supermarket Meal Planner API")
                .version("v1")
                .description("API independiente de demostración. Las mutaciones requieren CSRF; "
                        + "planes y listas pertenecen al usuario autenticado."));
    }
}
