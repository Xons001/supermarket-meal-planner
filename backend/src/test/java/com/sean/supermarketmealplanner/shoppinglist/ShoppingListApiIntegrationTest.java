package com.sean.supermarketmealplanner.shoppinglist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sean.supermarketmealplanner.AbstractIntegrationTest;
import com.sean.supermarketmealplanner.shoppinglist.infrastructure.persistence.ShoppingListRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
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
class ShoppingListApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private ShoppingListRepository repository;

    @Test
    void createsAggregatedListWithWholePackagesCostsSnapshotsAndPerformance() throws Exception {
        var planId = savedPlan(5001L);
        var response = create(planId);

        assertThat(response.get("mealPlanId").asText()).isEqualTo(planId.toString());
        assertThat(response.get("generationDurationMilliseconds").asLong()).isLessThan(500);
        assertThat(response.get("calculationComplete").asBoolean()).isTrue();
        assertThat(response.get("budgetCalculationComplete").asBoolean()).isTrue();
        assertThat(response.get("totalPackages").asInt()).isPositive();
        var items = flattenItems(response);
        assertThat(items).isNotEmpty();
        assertThat(items.stream().map(item -> item.get("productId").asText()).distinct().count())
                .isEqualTo(items.size());
        assertThat(totalIngredientOccurrences(planId)).isGreaterThan(items.size());

        for (var item : items) {
            var required = item.get("requiredQuantity").decimalValue();
            var purchased = item.get("purchasedQuantity").decimalValue();
            var leftover = item.get("leftoverQuantity").decimalValue();
            var packages = item.get("packagesRequired").asInt();
            var packageBase = packageBase(item);
            assertThat(packages).isEqualTo(required.divide(
                    packageBase,
                    0,
                    RoundingMode.CEILING
            ).intValue());
            assertThat(purchased).isGreaterThanOrEqualTo(required);
            assertThat(leftover).isEqualByComparingTo(purchased.subtract(required));
            assertThat(item.get("purchaseCost").decimalValue()).isEqualByComparingTo(
                    item.get("packagePrice").decimalValue()
                            .multiply(BigDecimal.valueOf(packages))
                            .setScale(2)
            );
        }
        assertThat(response.at("/quantitySummary/WEIGHT/unit").asText()).isEqualTo("GRAM");
        assertThat(response.at("/quantitySummary/VOLUME/unit").asText()).isEqualTo("MILLILITER");
        assertThat(response.at("/quantitySummary/UNIT/unit").asText()).isEqualTo("UNIT");

        var firstItem = items.getFirst();
        var originalName = firstItem.get("productName").asText();
        var originalPackagePrice = firstItem.get("packagePrice").decimalValue();
        var changedPlanSnapshot = snapshot(planId);
        ingredients(changedPlanSnapshot).stream()
                .filter(ingredient -> ingredient.get("productId").asText()
                        .equals(firstItem.get("productId").asText()))
                .forEach(ingredient -> {
                    ingredient.put("productName", "Nombre modificado después");
                    ingredient.put("packagePrice", new BigDecimal("999.99"));
                });
        updateSnapshot(planId, changedPlanSnapshot);

        mockMvc.perform(get("/api/v1/meal-plans/{id}/shopping-list", planId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(response.get("id").asText()));
        var persistedSnapshot = json(mockMvc.perform(get(
                        "/api/v1/shopping-lists/{id}",
                        response.get("id").asText()
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mealPlanId").value(planId.toString()))
                .andReturn().getResponse().getContentAsString());
        var unchangedItem = flattenItems(persistedSnapshot).stream()
                .filter(item -> item.get("productId").asText()
                        .equals(firstItem.get("productId").asText()))
                .findFirst()
                .orElseThrow();
        assertThat(unchangedItem.get("productName").asText()).isEqualTo(originalName);
        assertThat(unchangedItem.get("packagePrice").decimalValue())
                .isEqualByComparingTo(originalPackagePrice);
    }

    @Test
    void rejectsDuplicateCreationAndSupportsListingFiltersPaginationSortAndCsv() throws Exception {
        var planId = savedPlan(5002L);
        var list = create(planId);

        mockMvc.perform(post("/api/v1/meal-plans/{id}/shopping-list", planId))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errorCode").value("SHOPPING_LIST_ALREADY_EXISTS"));
        mockMvc.perform(get("/api/v1/shopping-lists")
                        .param("supermarketCode", "MERCADONA")
                        .param("status", "GENERATED")
                        .param("calculationComplete", "true")
                        .param("budgetExceeded", String.valueOf(
                                list.get("purchaseBudgetExceeded").asBoolean()
                        ))
                        .param("page", "0")
                        .param("size", "1")
                        .param("sort", "totalPurchaseCost,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(list.get("id").asText()))
                .andExpect(jsonPath("$.size").value(1));
        mockMvc.perform(get("/api/v1/shopping-lists/{id}/export", list.get("id").asText())
                        .param("format", "csv"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
                .andExpect(header().string(
                        "Content-Disposition",
                        org.hamcrest.Matchers.containsString("shopping-list-")
                ))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Categoría")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "Coste de compra"
                )));
    }

    @Test
    void regeneratesTransactionallyAndKeepsPreviousListAsInactiveHistory() throws Exception {
        var planId = savedPlan(5003L);
        var first = create(planId);
        jdbcTemplate.update(
                "UPDATE meal_plans SET content_version = content_version + 1 WHERE id = ?",
                planId
        );
        entityManager.clear();

        mockMvc.perform(get("/api/v1/meal-plans/{id}/shopping-list", planId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(first.get("id").asText()))
                .andExpect(jsonPath("$.status").value("GENERATED"))
                .andExpect(jsonPath("$.freshness").value("OUTDATED"));
        assertThat(repository.findById(UUID.fromString(first.get("id").asText()))
                .orElseThrow().getStatus().name()).isEqualTo("GENERATED");

        var replacement = json(mockMvc.perform(post(
                        "/api/v1/meal-plans/{id}/shopping-list/regenerate",
                        planId
                ))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        assertThat(replacement.get("id").asText()).isNotEqualTo(first.get("id").asText());
        var previous = repository.findById(UUID.fromString(first.get("id").asText())).orElseThrow();
        assertThat(previous.getStatus().name()).isEqualTo("GENERATED");
        assertThat(previous.isActive()).isFalse();
        assertThat(previous.isArchived()).isFalse();
        assertThat(repository.findByMealPlanIdAndActiveTrue(planId)
                .orElseThrow().getId().toString()).isEqualTo(replacement.get("id").asText());
        assertThat(replacement.get("freshness").asText()).isEqualTo("CURRENT");
        mockMvc.perform(get("/api/v1/shopping-lists/{id}", first.get("id").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(first.get("id").asText()))
                .andExpect(jsonPath("$.freshness").value("OUTDATED"))
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.archived").value(false));
    }

    @Test
    void failedRegenerationKeepsPreviousActiveList() throws Exception {
        var planId = savedPlan(5004L);
        var original = create(planId);
        jdbcTemplate.update("UPDATE meal_plans SET result_json = ? WHERE id = ?", "{", planId);
        entityManager.clear();

        mockMvc.perform(post("/api/v1/meal-plans/{id}/shopping-list/regenerate", planId))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("MEAL_PLAN_SNAPSHOT_INSUFFICIENT"));

        assertThat(repository.findByMealPlanIdAndActiveTrue(planId)
                .orElseThrow().getId().toString()).isEqualTo(original.get("id").asText());
    }

    @Test
    void legacySnapshotCreatesPartialListWithoutInventingPackageValues() throws Exception {
        var planId = savedPlan(5005L);
        var snapshot = snapshot(planId);
        ingredients(snapshot).forEach(ingredient -> {
            for (var field : List.of(
                    "brand", "categoryId", "categoryName", "measurementType",
                    "packageQuantity", "packageUnit", "packagePrice", "unitPrice",
                    "available", "consumedCost", "calculationComplete", "warnings",
                    "quantityBasis"
            )) {
                ingredient.remove(field);
            }
        });
        updateSnapshot(planId, snapshot);

        var response = create(planId);
        assertThat(response.get("calculationComplete").asBoolean()).isFalse();
        assertThat(response.get("budgetCalculationComplete").asBoolean()).isFalse();
        assertThat(flattenItems(response)).allSatisfy(item -> {
            assertThat(item.get("requiredQuantity").decimalValue()).isPositive();
            assertThat(item.get("packagesRequired").isNull()).isTrue();
            assertThat(item.get("purchaseCost").isNull()).isTrue();
            assertThat(item.get("available").isNull()).isTrue();
        });
        assertThat(response.toString()).contains("PRODUCT_SNAPSHOT_INCOMPLETE");
        assertThat(response.toString()).doesNotContain("PRODUCT_UNAVAILABLE");
    }

    @Test
    void unavailableSnapshotRemainsVisibleAndArchiveIsLogical() throws Exception {
        var planId = savedPlan(5006L);
        var snapshot = snapshot(planId);
        var first = ingredients(snapshot).getFirst();
        first.put("available", false);
        var productId = first.get("productId").asText();
        updateSnapshot(planId, snapshot);

        var response = create(planId);
        assertThat(flattenItems(response).stream()
                .filter(item -> item.get("productId").asText().equals(productId))
                .findFirst().orElseThrow().get("available").asBoolean()).isFalse();
        assertThat(response.toString()).contains("PRODUCT_UNAVAILABLE");

        mockMvc.perform(delete("/api/v1/meal-plans/{id}/shopping-list", planId))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/meal-plans/{id}/shopping-list", planId))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/shopping-lists/{id}", response.get("id").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"));
        mockMvc.perform(patch("/api/v1/meal-plans/{id}/shopping-list/status", planId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"GENERATED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("GENERATED"));
    }

    @Test
    void incompatibleRepeatedProductReturnsSafeProblemDetails() throws Exception {
        var planId = savedPlan(5007L);
        var snapshot = snapshot(planId);
        var seen = new HashMap<String, ObjectNode>();
        ObjectNode changed = null;
        for (var ingredient : ingredients(snapshot)) {
            var productId = ingredient.get("productId").asText();
            if (seen.containsKey(productId)) {
                ingredient.put("quantityUnit", "UNIT");
                ingredient.put("measurementType", "UNIT");
                ingredient.put("packageUnit", "UNIT");
                changed = ingredient;
                break;
            }
            seen.put(productId, ingredient);
        }
        assertThat(changed).isNotNull();
        var productId = changed.get("productId").asText();
        updateSnapshot(planId, snapshot);

        mockMvc.perform(post("/api/v1/meal-plans/{id}/shopping-list", planId))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("SHOPPING_LIST_UNIT_INCOMPATIBLE"))
                .andExpect(jsonPath("$.productId").value(productId))
                .andExpect(jsonPath("$.unitsDetected").isArray())
                .andExpect(jsonPath("$.expectedMeasurementType").exists());
        assertThat(repository.findByMealPlanIdAndActiveTrue(planId)).isEmpty();
    }

    @Test
    void requestedServingsAreAppliedBeforeShoppingListAggregation() throws Exception {
        var request = baseRequest(5008L);
        request.put("numberOfDays", 1);
        request.put("mealsPerDay", 1);
        request.put("servings", 2);
        request.put("allowedMealTypes", List.of("LUNCH"));
        var preview = generate(request);
        var meal = preview.at("/days/0/meals/0");
        var ingredient = meal.at("/ingredients/0");
        var templateValues = jdbcTemplate.queryForMap(
                """
                SELECT ingredient.quantity, template.servings
                FROM meal_template_ingredients ingredient
                JOIN meal_templates template ON template.id = ingredient.meal_template_id
                WHERE template.id = ? AND ingredient.product_id = ?
                """,
                UUID.fromString(meal.get("templateId").asText()),
                UUID.fromString(ingredient.get("productId").asText())
        );
        var expected = ((BigDecimal) templateValues.get("quantity"))
                .multiply(new BigDecimal("2"))
                .divide(BigDecimal.valueOf(((Number) templateValues.get("servings")).longValue()));
        assertThat(ingredient.get("quantity").decimalValue()).isEqualByComparingTo(expected);
        assertThat(ingredient.get("quantityBasis").asText()).isEqualTo("MEAL_TOTAL");

        request.put("persist", true);
        request.put("generationToken", preview.get("generationToken").asText());
        var planId = UUID.fromString(generate(request).get("mealPlanId").asText());
        var shoppingList = create(planId);
        var matching = flattenItems(shoppingList).stream()
                .filter(item -> item.get("productId").asText()
                        .equals(ingredient.get("productId").asText()))
                .findFirst()
                .orElseThrow();
        assertThat(matching.get("requiredQuantity").decimalValue())
                .isEqualByComparingTo(expected);
    }

    @Test
    void completePurchaseAwareSnapshotMatchesPersistedShoppingListExactly() throws Exception {
        var request = baseRequest(5010L);
        request.put("strategy", "PURCHASE_AWARE_SCORING");
        request.put("optimizationPreset", "BALANCED");
        var preview = generate(request);

        request.put("persist", true);
        request.put("generationToken", preview.get("generationToken").asText());
        var planId = UUID.fromString(generate(request).get("mealPlanId").asText());
        var shoppingList = create(planId);

        assertThat(preview.at("/purchaseMetrics/calculationComplete").asBoolean()).isTrue();
        assertThat(shoppingList.get("calculationComplete").asBoolean()).isTrue();
        assertThat(shoppingList.get("totalConsumedCost").decimalValue())
                .isEqualByComparingTo(
                        preview.at("/purchaseMetrics/estimatedConsumedCost").decimalValue()
                );
        assertThat(shoppingList.get("totalPurchaseCost").decimalValue())
                .isEqualByComparingTo(
                        preview.at("/purchaseMetrics/estimatedPurchaseCost").decimalValue()
                );
        assertThat(shoppingList.get("totalWasteCost").decimalValue())
                .isEqualByComparingTo(
                        preview.at("/purchaseMetrics/estimatedWasteCost").decimalValue()
                );
        assertThat(shoppingList.get("overallWastePercentage").decimalValue())
                .isEqualByComparingTo(
                        preview.at("/purchaseMetrics/estimatedWastePercentage").decimalValue()
                );
        assertThat(shoppingList.get("totalPackages").asInt())
                .isEqualTo(preview.at("/purchaseMetrics/estimatedPackageCount").asInt());
    }

    private UUID savedPlan(long seed) throws Exception {
        var request = baseRequest(seed);
        var preview = generate(request);
        request.put("persist", true);
        request.put("generationToken", preview.get("generationToken").asText());
        return UUID.fromString(generate(request).get("mealPlanId").asText());
    }

    private JsonNode generate(Map<String, Object> request) throws Exception {
        return json(mockMvc.perform(post("/api/v1/meal-plans/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }

    private JsonNode create(UUID planId) throws Exception {
        return json(mockMvc.perform(post("/api/v1/meal-plans/{id}/shopping-list", planId))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
    }

    private Map<String, Object> baseRequest(long seed) {
        var request = new LinkedHashMap<String, Object>();
        request.put("supermarketCode", "MERCADONA");
        request.put("name", "Plan para lista");
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
        request.put("persist", false);
        return request;
    }

    private ObjectNode snapshot(UUID planId) throws Exception {
        var raw = jdbcTemplate.queryForObject(
                "SELECT result_json FROM meal_plans WHERE id = ?",
                String.class,
                planId
        );
        return (ObjectNode) objectMapper.readTree(raw);
    }

    private List<ObjectNode> ingredients(JsonNode snapshot) {
        var result = new ArrayList<ObjectNode>();
        snapshot.get("days").forEach(day -> day.get("meals").forEach(meal ->
                meal.get("ingredients").forEach(ingredient -> result.add((ObjectNode) ingredient))
        ));
        return result;
    }

    private void updateSnapshot(UUID planId, JsonNode snapshot) throws Exception {
        jdbcTemplate.update(
                "UPDATE meal_plans SET result_json = ? WHERE id = ?",
                objectMapper.writeValueAsString(snapshot),
                planId
        );
        entityManager.clear();
    }

    private List<JsonNode> flattenItems(JsonNode response) {
        var items = new ArrayList<JsonNode>();
        response.get("groups").forEach(group -> group.get("items").forEach(items::add));
        return items;
    }

    private int totalIngredientOccurrences(UUID planId) {
        return jdbcTemplate.queryForObject(
                "SELECT COALESCE(sum(jsonb_array_length(pm.ingredients_json::jsonb)), 0) "
                        + "FROM planned_meals pm "
                        + "JOIN meal_plan_days d ON d.id = pm.meal_plan_day_id "
                        + "WHERE d.meal_plan_id = ?",
                Integer.class,
                planId
        );
    }

    private BigDecimal packageBase(JsonNode item) {
        var quantity = item.get("packageQuantity").decimalValue();
        return switch (item.get("packageUnit").asText()) {
            case "KG", "L" -> quantity.multiply(new BigDecimal("1000"));
            default -> quantity;
        };
    }

    private JsonNode json(String raw) throws Exception {
        return objectMapper.readTree(raw);
    }
}
