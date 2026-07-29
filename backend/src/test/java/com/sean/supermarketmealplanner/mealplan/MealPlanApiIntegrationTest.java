package com.sean.supermarketmealplanner.mealplan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sean.supermarketmealplanner.AbstractIntegrationTest;
import com.sean.supermarketmealplanner.catalog.infrastructure.persistence.ProductRepository;
import com.sean.supermarketmealplanner.mealplan.infrastructure.persistence.MealPlanRepository;
import com.sean.supermarketmealplanner.mealtemplate.infrastructure.persistence.MealTemplateRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@AutoConfigureMockMvc
@Transactional
class MealPlanApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MealPlanRepository repository;

    @Autowired
    private MealTemplateRepository templateRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void generatesSevenDayPreviewWithoutPersistingAndWithinPerformanceTarget() throws Exception {
        var before = repository.count();

        generate(baseRequest(123456L, false))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.persisted").value(false))
                .andExpect(jsonPath("$.mealPlanId").doesNotExist())
                .andExpect(jsonPath("$.days", hasSize(7)))
                .andExpect(jsonPath("$.days[*].meals", everyItem(hasSize(4))))
                .andExpect(jsonPath("$.seed").value(123456))
                .andExpect(jsonPath("$.strategy").value("PURCHASE_AWARE_SCORING"))
                .andExpect(jsonPath("$.overallScore").isNumber())
                .andExpect(jsonPath("$.scoreBreakdown.totalScore").isNumber())
                .andExpect(jsonPath("$.generationMetadata.durationMilliseconds").value(
                        org.hamcrest.Matchers.lessThan(2000)
                ));

        assertThat(repository.count()).isEqualTo(before);
    }

    @Test
    void generatedSeedIsSafeForTheTypedJavascriptClient() throws Exception {
        var request = baseRequest(1L, false);
        request.remove("deterministicSeed");
        var response = result(request);

        assertThat(response.get("seed").asLong())
                .isBetween(-9_007_199_254_740_991L, 9_007_199_254_740_991L);
    }

    @Test
    void sameSeedIsDeterministicAndDifferentSeedsRemainTraceable() throws Exception {
        var first = result(baseRequest(77L, false));
        var second = result(baseRequest(77L, false));
        var different = result(baseRequest(78L, false));

        assertThat(first.at("/days").toString()).isEqualTo(second.at("/days").toString());
        assertThat(first.get("generationToken").asText())
                .isEqualTo(second.get("generationToken").asText());
        assertThat(different.get("seed").asLong()).isEqualTo(78L);
        assertThat(different.get("generationToken").asText())
                .isNotEqualTo(first.get("generationToken").asText());
    }

    @Test
    void appliesMealTypePreparationTemplateAndProductExclusions() throws Exception {
        var excludedTemplate = templateRepository.findAllByArchivedFalse().stream()
                .filter(template -> template.getPreparationMinutes() <= 30)
                .findFirst()
                .orElseThrow();
        var excludedProduct = productRepository.findAll().stream()
                .filter(product -> product.isAvailable())
                .findFirst()
                .orElseThrow();
        var request = baseRequest(90L, false);
        request.put("numberOfDays", 2);
        request.put("mealsPerDay", 2);
        request.put("allowedMealTypes", List.of("LUNCH"));
        request.put("maximumPreparationMinutes", 30);
        request.put("excludedTemplateIds", List.of(excludedTemplate.getId()));
        request.put("excludedProductIds", List.of(excludedProduct.getId()));

        var response = result(request);
        response.at("/days").forEach(day -> day.at("/meals").forEach(meal -> {
            assertThat(meal.get("mealType").asText()).isEqualTo("LUNCH");
            assertThat(meal.get("preparationMinutes").asInt()).isLessThanOrEqualTo(30);
            assertThat(meal.get("templateId").asText())
                    .isNotEqualTo(excludedTemplate.getId().toString());
            meal.at("/ingredients").forEach(ingredient -> assertThat(
                    ingredient.get("productId").asText()
            ).isNotEqualTo(excludedProduct.getId().toString()));
        }));
    }

    @Test
    void distributesEverySupportedMealsPerDayDeterministically() throws Exception {
        var expected = List.of(
                List.of("LUNCH"),
                List.of("LUNCH", "DINNER"),
                List.of("BREAKFAST", "LUNCH", "DINNER"),
                List.of("BREAKFAST", "LUNCH", "SNACK", "DINNER"),
                List.of("BREAKFAST", "SNACK", "LUNCH", "SNACK", "DINNER"),
                List.of("BREAKFAST", "SNACK", "LUNCH", "SNACK", "DINNER", "SNACK")
        );
        for (int mealsPerDay = 1; mealsPerDay <= 6; mealsPerDay++) {
            var request = baseRequest(100L + mealsPerDay, false);
            request.put("numberOfDays", 1);
            request.put("mealsPerDay", mealsPerDay);
            var response = result(request);
            var actual = new ArrayList<String>();
            response.at("/days/0/meals").forEach(meal ->
                    actual.add(meal.get("mealType").asText()));
            assertThat(actual).isEqualTo(expected.get(mealsPerDay - 1));
        }
    }

    @Test
    void appliesDietaryTagsAndMandatoryAllergens() throws Exception {
        var vegan = baseRequest(301L, false);
        vegan.put("numberOfDays", 2);
        vegan.put("mealsPerDay", 1);
        vegan.put("allowedMealTypes", List.of("LUNCH"));
        vegan.put("requiredDietaryTags", List.of("HIGH_PROTEIN"));
        var veganResult = result(vegan);
        assertThat(veganResult.at("/constraintsApplied").toString()).contains("HIGH_PROTEIN");

        var fishFree = baseRequest(302L, false);
        fishFree.put("numberOfDays", 2);
        fishFree.put("mealsPerDay", 1);
        fishFree.put("allowedMealTypes", List.of("LUNCH"));
        fishFree.put("excludedAllergens", List.of("FISH"));
        var fishFreeResult = result(fishFree);
        assertThat(fishFreeResult.at("/constraintsApplied").toString()).contains("FISH");
        fishFreeResult.at("/days").forEach(day -> day.at("/meals").forEach(meal ->
                meal.at("/ingredients").forEach(ingredient ->
                        assertThat(ingredient.get("productName").asText())
                                .doesNotContainIgnoringCase("atún"))));
    }

    @Test
    void incompleteTemplatesAreRejectedByDefaultAndPenalizedWhenAllowed() throws Exception {
        var incompleteProduct = productRepository.findAll().stream()
                .filter(product -> product.getExternalId().equals(
                        "demo-mercadona-apple"
                ))
                .findFirst()
                .orElseThrow();
        var ingredient = new LinkedHashMap<String, Object>();
        ingredient.put("productId", incompleteProduct.getId());
        ingredient.put("quantity", 120);
        ingredient.put("quantityUnit", "GRAM");
        ingredient.put("optional", false);
        ingredient.put("sortOrder", 0);
        var templateRequest = new LinkedHashMap<String, Object>();
        templateRequest.put("supermarketCode", "MERCADONA");
        templateRequest.put("name", "Snack incompleto para el generador");
        templateRequest.put("description", "Plantilla controlada para pruebas");
        templateRequest.put("mealType", "SNACK");
        templateRequest.put("instructions", List.of("Preparar y servir."));
        templateRequest.put("preparationMinutes", 5);
        templateRequest.put("servings", 1);
        templateRequest.put("active", true);
        templateRequest.put("ingredients", List.of(ingredient));
        var created = mockMvc.perform(post("/api/v1/meal-templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(templateRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        var incompleteTemplateId = UUID.fromString(
                objectMapper.readTree(created).get("id").asText()
        );
        var excluded = templateRepository.findAllByArchivedFalse().stream()
                .filter(template -> !template.getId().equals(incompleteTemplateId))
                .map(template -> template.getId().toString())
                .toList();
        var strict = baseRequest(401L, false);
        strict.put("numberOfDays", 1);
        strict.put("mealsPerDay", 1);
        strict.put("allowedMealTypes", List.of("SNACK"));
        strict.put("excludedTemplateIds", excluded);

        generate(strict).andExpect(status().isUnprocessableEntity());

        strict.put("allowIncompleteCalculations", true);
        var allowed = result(strict);
        assertThat(allowed.get("calculationComplete").asBoolean()).isFalse();
        assertThat(allowed.at("/scoreBreakdown/completenessScore").decimalValue())
                .isLessThan(new java.math.BigDecimal("100"));
        assertThat(allowed.at("/warnings").toString()).contains("INCOMPLETE_CALCULATION");
    }

    @Test
    void rejectsInvalidSupermarketsReferencesAndBudgets() throws Exception {
        var invalidSupermarket = baseRequest(1L, false);
        invalidSupermarket.put("supermarketCode", "CARREFOUR");
        generate(invalidSupermarket)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("disabled supermarket")));

        var invalidReferences = baseRequest(1L, false);
        invalidReferences.put("requiredDietaryTags", List.of("UNKNOWN"));
        invalidReferences.put("excludedAllergens", List.of("UNKNOWN"));
        generate(invalidReferences)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("Invalid dietary tags")));

        var missingTemplate = baseRequest(1L, false);
        missingTemplate.put("excludedTemplateIds", List.of(UUID.randomUUID()));
        generate(missingTemplate)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("not found")));

        var invalidBudget = baseRequest(1L, false);
        invalidBudget.put("weeklyBudget", 0);
        generate(invalidBudget).andExpect(status().isBadRequest());
    }

    @Test
    void reportsImpossibleGenerationWithProblemDetailsAndDiagnostics() throws Exception {
        var request = baseRequest(1L, false);
        request.put("excludedTemplateIds", templateRepository.findAllByArchivedFalse().stream()
                .map(template -> template.getId().toString())
                .toList());

        generate(request)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errorCode").value("MEAL_PLAN_GENERATION_IMPOSSIBLE"))
                .andExpect(jsonPath("$.detail", containsString("No meal templates")))
                .andExpect(jsonPath("$.candidateCounts").exists())
                .andExpect(jsonPath("$.suggestions").isArray());
    }

    @Test
    void persistsExactlyThePreviewWhenTokenAndSeedMatch() throws Exception {
        var preview = result(baseRequest(2026L, false));
        var request = baseRequest(2026L, true);
        request.put("generationToken", preview.get("generationToken").asText());
        var saved = result(request);

        assertThat(saved.get("persisted").asBoolean()).isTrue();
        assertThat(withoutPersistentIds(saved.at("/days")).toString())
                .isEqualTo(withoutPersistentIds(preview.at("/days")).toString());
        assertThat(saved.at("/scoreBreakdown").toString())
                .isEqualTo(preview.at("/scoreBreakdown").toString());
        assertThat(saved.get("generationToken").asText())
                .isEqualTo(preview.get("generationToken").asText());
        assertThat(repository.count()).isEqualTo(1);

        var id = UUID.fromString(saved.get("mealPlanId").asText());
        assertThat(count("meal_plan_days", id)).isEqualTo(7);
        assertThat(count("planned_meals", id)).isEqualTo(28);
        assertThat(count("meal_plan_warnings", id)).isGreaterThan(0);
    }

    @Test
    void rejectsPersistenceWhenGenerationTokenDoesNotMatch() throws Exception {
        var request = baseRequest(2026L, true);
        request.put("generationToken", "0".repeat(64));

        generate(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("generationToken")));
        assertThat(repository.count()).isZero();
    }

    @Test
    void listsFiltersGetsArchivesAndReactivatesSnapshots() throws Exception {
        var saved = result(baseRequest(44L, true));
        var id = saved.get("mealPlanId").asText();

        mockMvc.perform(get("/api/v1/meal-plans")
                        .queryParam("supermarketCode", "MERCADONA")
                        .queryParam("status", "GENERATED")
                        .queryParam("minimumScore", "0")
                        .queryParam("startDateFrom", "2026-08-01")
                        .queryParam("startDateTo", "2026-08-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(id));

        mockMvc.perform(get("/api/v1/meal-plans/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days", hasSize(7)));

        mockMvc.perform(delete("/api/v1/meal-plans/{id}", id))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/meal-plans/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"));

        mockMvc.perform(patch("/api/v1/meal-plans/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"GENERATED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("GENERATED"));
    }

    @Test
    void validatesRangesFiltersAndUnknownIdsAsProblemDetails() throws Exception {
        var request = baseRequest(1L, false);
        request.put("numberOfDays", 15);
        generate(request)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        mockMvc.perform(get("/api/v1/meal-plans").queryParam("minimumScore", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BAD_REQUEST"));
        mockMvc.perform(get("/api/v1/meal-plans/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
    }

    @Test
    void classicStrategyRemainsEquivalentAcrossRepresentativeFixturesAndSeeds()
            throws Exception {
        assertClassicBaseline(
                classicRequest("one-meal-budget", 11, 3, 1, 25, "MEDIUM", 3, 40),
                "e35196c1f472bf37bbd040ccf352fe7e31a0e00d3d9bbd9d1020474c57f733c9",
                "48.89",
                "4.56"
        );
        var noBudget = classicRequest(
                "three-meals-no-budget", 29, 4, 3, null, "HIGH", 2, 35
        );
        assertClassicBaseline(
                noBudget,
                "0dca893d34f7ebbe25515c536699c340f4d612690c11b5afbb1ef5e273817561",
                "62.18",
                "17.34"
        );
        assertClassicBaseline(
                classicRequest("four-meals-standard", 123456, 7, 4, 70, "HIGH", 3, 40),
                "65063f6fcea4bddea4a7bdce275ea5d92bb6b4bbd1a2778452f78f8141b7df8e",
                "65.80",
                "38.08"
        );
        assertClassicBaseline(
                classicRequest("six-meals-tight", 98765, 2, 6, 20, "LOW", 4, 30),
                "6e2e54e46e8d3e28f861185b1f5269016f5717fb17e3314515ba8260cc6180df",
                "85.37",
                "12.99"
        );
        assertClassicBaseline(
                classicRequest("four-meals-other-seed", 777, 7, 4, 55, "MEDIUM", 2, 25),
                "ce537d13a530780f395697cdf602cb30084456409e2989c81d5f7d29b8b36442",
                "54.40",
                "35.22"
        );
    }

    @Test
    void purchaseAwarePreviewExposesPackageMetricsWeightsAndPreset() throws Exception {
        var request = baseRequest(123456L, false);
        request.put("strategy", "PURCHASE_AWARE_SCORING");
        request.put("optimizationPreset", "LOWER_WASTE");

        generate(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.strategy").value("PURCHASE_AWARE_SCORING"))
                .andExpect(jsonPath("$.purchaseMetrics.estimatedPurchaseCost").isNumber())
                .andExpect(jsonPath("$.purchaseMetrics.estimatedWasteCost").isNumber())
                .andExpect(jsonPath("$.purchaseMetrics.estimatedPackageCount").isNumber())
                .andExpect(jsonPath("$.purchaseMetrics.estimatedUniqueProductCount").isNumber())
                .andExpect(jsonPath("$.scoreBreakdown.purchaseCostScore").isNumber())
                .andExpect(jsonPath("$.scoreBreakdown.usefulReuseScore").isNumber())
                .andExpect(jsonPath("$.generationMetadata.optimizationPreset")
                        .value("LOWER_WASTE"))
                .andExpect(jsonPath("$.generationMetadata.scoreWeights.wasteCost")
                        .value(12));
    }

    @Test
    void classicStrategyNormalizesOptimizationPresetToNull() throws Exception {
        var request = baseRequest(123456L, false);
        request.put("strategy", "SCORING");
        request.put("optimizationPreset", "MORE_REUSE");

        generate(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.strategy").value("SCORING"))
                .andExpect(jsonPath("$.purchaseMetrics").doesNotExist())
                .andExpect(jsonPath("$.generationMetadata.optimizationPreset").doesNotExist());
    }

    @Test
    void readsHistoricalSnapshotsWithoutPurchaseAwareFields() throws Exception {
        var request = baseRequest(8080L, false);
        request.put("strategy", "SCORING");
        var preview = result(request);
        request.put("persist", true);
        request.put("generationToken", preview.get("generationToken").asText());
        var saved = result(request);
        var planId = UUID.fromString(saved.get("mealPlanId").asText());
        var snapshot = (ObjectNode) saved.deepCopy();
        snapshot.remove("purchaseMetrics");
        var breakdown = (ObjectNode) snapshot.get("scoreBreakdown");
        List.of(
                "purchaseCostScore",
                "consumedCostScore",
                "purchaseBudgetScore",
                "wasteCostScore",
                "wastePercentageScore",
                "usefulReuseScore",
                "uniqueProductsScore",
                "packageCountScore"
        ).forEach(breakdown::remove);
        var metadata = (ObjectNode) snapshot.get("generationMetadata");
        metadata.remove("optimizationPreset");
        metadata.remove("scoreWeights");
        jdbcTemplate.update(
                "UPDATE meal_plans SET result_json = ? WHERE id = ?",
                objectMapper.writeValueAsString(snapshot),
                planId
        );

        mockMvc.perform(get("/api/v1/meal-plans/{id}", planId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.strategy").value("SCORING"))
                .andExpect(jsonPath("$.purchaseMetrics").doesNotExist())
                .andExpect(jsonPath("$.scoreBreakdown.purchaseCostScore").doesNotExist());
    }

    @Test
    void editsOneMealWithVersionsLocksHistoryAndUndo() throws Exception {
        var preview = result(baseRequest(6060L, false));
        var request = baseRequest(6060L, true);
        request.put("generationToken", preview.get("generationToken").asText());
        var saved = result(request);
        var planId = saved.get("mealPlanId").asText();
        var meal = saved.at("/days/0/meals/0");
        var mealId = meal.get("plannedMealId").asText();
        var originalTemplate = meal.get("templateId").asText();

        var locked = edit(
                patch("/api/v1/meal-plans/{planId}/meals/{mealId}/lock", planId, mealId),
                Map.of("locked", true, "expectedEditVersion", 0)
        );
        assertThat(locked.get("editVersion").asLong()).isEqualTo(1);
        assertThat(locked.get("contentVersion").asLong()).isZero();
        assertThat(locked.at("/days/0/meals/0/locked").asBoolean()).isTrue();

        var unlocked = edit(
                patch("/api/v1/meal-plans/{planId}/meals/{mealId}/lock", planId, mealId),
                Map.of("locked", false, "expectedEditVersion", 1)
        );
        assertThat(unlocked.get("editVersion").asLong()).isEqualTo(2);
        assertThat(unlocked.get("contentVersion").asLong()).isZero();

        var alternatives = objectMapper.readTree(mockMvc.perform(
                        get("/api/v1/meal-plans/{planId}/meals/{mealId}/alternatives", planId, mealId)
                                .param("seed", "9191")
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertThat(alternatives.size()).isPositive();

        var replacementPreview = edit(
                post("/api/v1/meal-plans/{planId}/meals/{mealId}/replacement-previews",
                        planId, mealId),
                Map.of(
                        "mealTemplateId", alternatives.get(0).get("mealTemplateId").asText(),
                        "expectedEditVersion", 2,
                        "seed", 9191
                )
        );
        var replaced = edit(
                post("/api/v1/meal-plans/{planId}/meals/{mealId}/replacements", planId, mealId),
                Map.of(
                        "previewToken", replacementPreview.get("previewToken").asText(),
                        "expectedEditVersion", 2
                )
        );
        assertThat(replaced.get("editVersion").asLong()).isEqualTo(3);
        assertThat(replaced.get("contentVersion").asLong()).isEqualTo(1);
        assertThat(replaced.at("/days/0/meals/0/templateId").asText())
                .isNotEqualTo(originalTemplate);
        assertThat(replaced.get("canUndo").asBoolean()).isTrue();

        mockMvc.perform(get("/api/v1/meal-plans/{planId}/changes", planId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.content[0].type").value("MEAL_REPLACED"));

        var restored = edit(
                post("/api/v1/meal-plans/{planId}/undo", planId),
                Map.of("expectedEditVersion", 3)
        );
        assertThat(restored.get("editVersion").asLong()).isEqualTo(4);
        assertThat(restored.get("contentVersion").asLong()).isEqualTo(2);
        assertThat(restored.at("/days/0/meals/0/templateId").asText())
                .isEqualTo(originalTemplate);
        assertThat(restored.at("/days/0/meals/0/locked").asBoolean()).isFalse();
    }

    @Test
    void regeneratesAndUndoesAWholeDayWithOneVersionIncrement() throws Exception {
        var preview = result(baseRequest(7070L, false));
        var request = baseRequest(7070L, true);
        request.put("generationToken", preview.get("generationToken").asText());
        var saved = result(request);
        var planId = saved.get("mealPlanId").asText();
        var dayId = saved.at("/days/0/dayId").asText();
        var originalDay = templateSequence(saved).subList(0, 4);

        var dayPreview = edit(
                post("/api/v1/meal-plans/{planId}/days/{dayId}/regeneration-previews",
                        planId, dayId),
                Map.of("expectedEditVersion", 0, "seed", 8181)
        );
        assertThat(dayPreview.at("/afterMeals").size()).isEqualTo(4);

        var regenerated = edit(
                post("/api/v1/meal-plans/{planId}/days/{dayId}/regenerations", planId, dayId),
                Map.of(
                        "previewToken", dayPreview.get("previewToken").asText(),
                        "expectedEditVersion", 0
                )
        );
        assertThat(regenerated.get("editVersion").asLong()).isEqualTo(1);
        assertThat(regenerated.get("contentVersion").asLong()).isEqualTo(1);
        assertThat(templateSequence(regenerated).subList(0, 4)).isNotEqualTo(originalDay);

        var restored = edit(
                post("/api/v1/meal-plans/{planId}/undo", planId),
                Map.of("expectedEditVersion", 1)
        );
        assertThat(restored.get("editVersion").asLong()).isEqualTo(2);
        assertThat(restored.get("contentVersion").asLong()).isEqualTo(2);
        assertThat(templateSequence(restored).subList(0, 4)).isEqualTo(originalDay);
    }

    @Test
    void returnsStableProblemCodesForMalformedTokensVersionsAndLocks() throws Exception {
        var preview = result(baseRequest(9090L, false));
        var request = baseRequest(9090L, true);
        request.put("generationToken", preview.get("generationToken").asText());
        var saved = result(request);
        var planId = saved.get("mealPlanId").asText();
        var mealId = saved.at("/days/0/meals/0/plannedMealId").asText();

        mockMvc.perform(post("/api/v1/meal-plans/{planId}/meals/{mealId}/replacements",
                        planId, mealId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "previewToken", "malformed",
                                "expectedEditVersion", 0
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errorCode").value("EDIT_PREVIEW_TOKEN_MALFORMED"));

        mockMvc.perform(patch("/api/v1/meal-plans/{planId}/meals/{mealId}/lock",
                        planId, mealId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "locked", true,
                                "expectedEditVersion", 99
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("MEAL_PLAN_VERSION_CONFLICT"));

        edit(
                patch("/api/v1/meal-plans/{planId}/meals/{mealId}/lock", planId, mealId),
                Map.of("locked", true, "expectedEditVersion", 0)
        );
        mockMvc.perform(post(
                        "/api/v1/meal-plans/{planId}/meals/{mealId}/regeneration-previews",
                        planId, mealId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("expectedEditVersion", 1))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("PLANNED_MEAL_LOCKED"));
    }

    private JsonNode edit(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder,
            Object body
    ) throws Exception {
        var response = mockMvc.perform(builder
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(body)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }

    private org.springframework.test.web.servlet.ResultActions generate(Map<String, Object> request)
            throws Exception {
        return mockMvc.perform(post("/api/v1/meal-plans/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)));
    }

    private Map<String, Object> classicRequest(
            String suffix,
            long seed,
            int days,
            int meals,
            Integer budget,
            String variety,
            int maximumRepetitions,
            int preparationMinutes
    ) {
        var request = baseRequest(seed, false);
        request.put("name", "Baseline " + suffix);
        request.put("numberOfDays", days);
        request.put("mealsPerDay", meals);
        request.put("maximumPreparationMinutes", preparationMinutes);
        request.put("maximumTemplateRepetitions", maximumRepetitions);
        request.put("varietyPreference", variety);
        request.put("strategy", "SCORING");
        if (budget == null) {
            request.remove("weeklyBudget");
        } else {
            request.put("weeklyBudget", budget);
        }
        return request;
    }

    private void assertClassicBaseline(
            Map<String, Object> request,
            String ignoredEnvironmentSpecificToken,
            String score,
            String cost
    ) throws Exception {
        var response = result(request);
        var repeated = result(request);
        assertThat(response.get("strategy").asText()).isEqualTo("SCORING");
        assertThat(response.get("generationToken").asText()).hasSize(64)
                .isEqualTo(repeated.get("generationToken").asText());
        assertThat(templateSequence(response)).isEqualTo(templateSequence(repeated));
        assertThat(response.get("overallScore").decimalValue()).isEqualByComparingTo(score);
        assertThat(response.get("totalConsumedCost").decimalValue()).isEqualByComparingTo(cost);
        assertThat(response.path("purchaseMetrics").isNull()).isTrue();
    }

    private JsonNode result(Map<String, Object> request) throws Exception {
        var body = generate(request)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(body);
    }

    private Map<String, Object> baseRequest(long seed, boolean persist) {
        var request = new LinkedHashMap<String, Object>();
        request.put("supermarketCode", "MERCADONA");
        request.put("name", "Plan de integración");
        request.put("startDate", "2026-08-03");
        request.put("numberOfDays", 7);
        request.put("mealsPerDay", 4);
        request.put("servings", 1);
        request.put("dailyCaloriesTarget", 2000);
        request.put("dailyProteinTarget", 100);
        request.put("weeklyBudget", 70);
        request.put("allowedMealTypes", List.of("BREAKFAST", "LUNCH", "SNACK", "DINNER"));
        request.put("requiredDietaryTags", List.of());
        request.put("excludedAllergens", List.of());
        request.put("excludedTemplateIds", new ArrayList<>());
        request.put("excludedProductIds", new ArrayList<>());
        request.put("maximumPreparationMinutes", 40);
        request.put("maximumTemplateRepetitions", 3);
        request.put("varietyPreference", "HIGH");
        request.put("allowIncompleteCalculations", false);
        request.put("deterministicSeed", seed);
        request.put("persist", persist);
        return request;
    }

    private List<String> templateSequence(JsonNode response) {
        var result = new ArrayList<String>();
        response.at("/days").forEach(day -> day.at("/meals")
                .forEach(meal -> result.add(meal.get("templateId").asText())));
        return result;
    }

    private JsonNode withoutPersistentIds(JsonNode days) {
        var copy = days.deepCopy();
        copy.forEach(day -> {
            ((ObjectNode) day).remove("dayId");
            day.at("/meals").forEach(meal -> ((ObjectNode) meal).remove("plannedMealId"));
        });
        return copy;
    }

    private int count(String table, UUID planId) {
        var foreignKey = table.equals("meal_plan_days") || table.equals("meal_plan_warnings")
                ? "meal_plan_id"
                : "meal_plan_day_id";
        if (table.equals("planned_meals")) {
            return jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM planned_meals pm "
                            + "JOIN meal_plan_days d ON d.id = pm.meal_plan_day_id "
                            + "WHERE d.meal_plan_id = ?",
                    Integer.class,
                    planId
            );
        }
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM " + table + " WHERE " + foreignKey + " = ?",
                Integer.class,
                planId
        );
    }
}
