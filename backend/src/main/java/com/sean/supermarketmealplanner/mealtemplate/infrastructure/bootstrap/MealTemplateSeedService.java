package com.sean.supermarketmealplanner.mealtemplate.infrastructure.bootstrap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sean.supermarketmealplanner.catalog.infrastructure.persistence.ProductRepository;
import com.sean.supermarketmealplanner.mealtemplate.domain.MealType;
import com.sean.supermarketmealplanner.mealtemplate.domain.QuantityUnit;
import com.sean.supermarketmealplanner.mealtemplate.infrastructure.persistence.MealTemplateEntity;
import com.sean.supermarketmealplanner.mealtemplate.infrastructure.persistence.MealTemplateRepository;
import com.sean.supermarketmealplanner.supermarket.domain.SupermarketCode;
import com.sean.supermarketmealplanner.supermarket.infrastructure.persistence.SupermarketRepository;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MealTemplateSeedService {

    private final ObjectMapper objectMapper;
    private final Resource resource;
    private final SupermarketRepository supermarketRepository;
    private final ProductRepository productRepository;
    private final MealTemplateRepository mealTemplateRepository;

    public MealTemplateSeedService(
            ObjectMapper objectMapper,
            @Value("${app.meal-templates.demo-resource}") Resource resource,
            SupermarketRepository supermarketRepository,
            ProductRepository productRepository,
            MealTemplateRepository mealTemplateRepository
    ) {
        this.objectMapper = objectMapper;
        this.resource = resource;
        this.supermarketRepository = supermarketRepository;
        this.productRepository = productRepository;
        this.mealTemplateRepository = mealTemplateRepository;
    }

    @Transactional
    public void importTemplates() {
        var document = read();
        if (!"DEMO".equals(document.classification())) {
            throw new IllegalStateException("Meal template seed data must be classified as DEMO");
        }
        var supermarketCode = SupermarketCode.valueOf(document.supermarketCode());
        var supermarket = supermarketRepository.findByCode(supermarketCode)
                .orElseThrow(() -> new IllegalStateException(
                        "Missing supermarket for meal template seeds: " + supermarketCode
        ));
        for (var seed : document.templates()) {
            var existing = mealTemplateRepository.findBySupermarketIdAndNameIgnoreCase(
                    supermarket.getId(),
                    seed.name()
            );
            var template = existing.orElseGet(() -> new MealTemplateEntity(supermarket, true));
            if (existing.isPresent()) {
                template.clearCollections();
                mealTemplateRepository.flush();
            }
            template.update(
                    seed.name(),
                    seed.description(),
                    MealType.valueOf(seed.mealType()),
                    seed.instructions(),
                    seed.preparationMinutes(),
                    seed.servings(),
                    seed.active(),
                    null
            );
            template.replaceIngredients(seed.ingredients().stream()
                    .map(ingredient -> {
                        var product = productRepository.findBySupermarketIdAndExternalId(
                                supermarket.getId(),
                                ingredient.productExternalId()
                        ).orElseThrow(() -> new IllegalStateException(
                                "Unknown seed product: " + ingredient.productExternalId()
                        ));
                        var quantityUnit = QuantityUnit.valueOf(ingredient.quantityUnit());
                        if (!quantityUnit.isCompatibleWith(product.getMeasurementType())) {
                            throw new IllegalStateException(
                                    "Incompatible seed unit for " + ingredient.productExternalId()
                            );
                        }
                        return new MealTemplateEntity.IngredientData(
                                product,
                                ingredient.quantity(),
                                quantityUnit,
                                ingredient.optional(),
                                ingredient.sortOrder(),
                                ingredient.notes()
                        );
                    })
                    .toList());
            mealTemplateRepository.save(template);
        }
    }

    private MealTemplateSeedDocument read() {
        try (var input = resource.getInputStream()) {
            return objectMapper.readValue(input, MealTemplateSeedDocument.class);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read meal template demo data", exception);
        }
    }
}
