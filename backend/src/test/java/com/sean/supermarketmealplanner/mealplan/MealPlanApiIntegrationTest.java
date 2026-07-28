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
                .andExpect(jsonPath("$.strategy").value("SCORING"))
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
        assertThat(saved.at("/days").toString()).isEqualTo(preview.at("/days").toString());
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

    private org.springframework.test.web.servlet.ResultActions generate(Map<String, Object> request)
            throws Exception {
        return mockMvc.perform(post("/api/v1/meal-plans/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)));
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
