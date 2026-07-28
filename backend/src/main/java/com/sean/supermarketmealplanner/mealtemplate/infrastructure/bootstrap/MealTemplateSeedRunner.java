package com.sean.supermarketmealplanner.mealtemplate.infrastructure.bootstrap;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
@ConditionalOnProperty(
        name = "app.meal-templates.seed-enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class MealTemplateSeedRunner implements ApplicationRunner {

    private final MealTemplateSeedService seedService;

    public MealTemplateSeedRunner(MealTemplateSeedService seedService) {
        this.seedService = seedService;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedService.importTemplates();
    }
}
