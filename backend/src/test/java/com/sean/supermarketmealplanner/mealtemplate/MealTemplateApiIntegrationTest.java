package com.sean.supermarketmealplanner.mealtemplate;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sean.supermarketmealplanner.AbstractIntegrationTest;
import com.sean.supermarketmealplanner.catalog.infrastructure.persistence.ProductEntity;
import com.sean.supermarketmealplanner.catalog.infrastructure.persistence.ProductRepository;
import com.sean.supermarketmealplanner.mealtemplate.infrastructure.bootstrap.MealTemplateSeedService;
import com.sean.supermarketmealplanner.mealtemplate.infrastructure.persistence.MealTemplateRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
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
class MealTemplateApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private MealTemplateRepository mealTemplateRepository;

    @Autowired
    private MealTemplateSeedService seedService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void seedsSixteenTemplatesIdempotentlyAndListsCalculatedValues() throws Exception {
        assertThatTemplateCountIsSixteen();
        seedService.importTemplates();
        assertThatTemplateCountIsSixteen();

        mockMvc.perform(get("/api/v1/meal-templates")
                        .queryParam("size", "9")
                        .queryParam("sort", "name,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(9)))
                .andExpect(jsonPath("$.totalElements").value(16))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content[*].demoData", everyItem(is(true))))
                .andExpect(jsonPath("$.content[0].nutritionPerServing.calories").isNumber())
                .andExpect(jsonPath("$.content[0].consumedCostPerServing").isNumber());
    }

    @Test
    void calculatesWeightVolumeUnitsServingsAndHalfUpRounding() throws Exception {
        var chicken = product("demo-mercadona-chicken-breast");
        var milk = product("demo-mercadona-lactose-free-milk");
        var eggs = product("demo-mercadona-eggs");

        preview(singleIngredientRequest(chicken, "125", "GRAM", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalNutrition.calories").value(137.5))
                .andExpect(jsonPath("$.totalNutrition.protein").value(28.9))
                .andExpect(jsonPath("$.totalConsumedCost").value(1.19))
                .andExpect(jsonPath("$.nutritionComplete").value(true))
                .andExpect(jsonPath("$.costComplete").value(true));

        preview(singleIngredientRequest(milk, "250", "MILLILITER", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalNutrition.calories").value(115.0))
                .andExpect(jsonPath("$.totalConsumedCost").value(0.29));

        preview(singleIngredientRequest(eggs, "2", "UNIT", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalNutrition.calories").value(156.0))
                .andExpect(jsonPath("$.totalNutrition.protein").value(13.8))
                .andExpect(jsonPath("$.totalConsumedCost").value(0.40));

        preview(singleIngredientRequest(chicken, "125", "GRAM", 2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nutritionPerServing.calories").value(68.8))
                .andExpect(jsonPath("$.consumedCostPerServing").value(0.59));
    }

    @Test
    void returnsPartialCalculationsWithoutFailing() throws Exception {
        var veganBurger = product("demo-mercadona-vegan-burger");

        preview(singleIngredientRequest(veganBurger, "120", "GRAM", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nutritionComplete").value(false))
                .andExpect(jsonPath("$.costComplete").value(false))
                .andExpect(jsonPath("$.calculationComplete").value(false))
                .andExpect(jsonPath("$.ingredients[0].calculatedNutrition").doesNotExist())
                .andExpect(jsonPath("$.ingredients[0].calculatedConsumedCost").doesNotExist())
                .andExpect(jsonPath("$.warnings[*]", hasItem(containsString(
                        "No hay información nutricional"
                ))))
                .andExpect(jsonPath("$.warnings[*]", hasItem(containsString(
                        "coste no se puede calcular"
                ))));
    }

    @Test
    void supportsDetailSearchFiltersPaginationAndSorting() throws Exception {
        var template = mealTemplateRepository.findAllByArchivedFalse().stream()
                .filter(item -> item.getName().equals("Arroz con pollo y espinacas"))
                .findFirst()
                .orElseThrow();

        mockMvc.perform(get("/api/v1/meal-templates/{id}", template.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Arroz con pollo y espinacas"))
                .andExpect(jsonPath("$.ingredients", hasSize(3)))
                .andExpect(jsonPath("$.instructions", hasSize(4)))
                .andExpect(jsonPath("$.totalNutrition.protein").isNumber());

        mockMvc.perform(get("/api/v1/meal-templates")
                        .queryParam("query", "POLLO")
                        .queryParam("mealType", "LUNCH")
                        .queryParam("active", "true")
                        .queryParam("maximumPreparationMinutes", "30")
                        .queryParam("minimumProtein", "20")
                        .queryParam("maximumCalories", "900")
                        .queryParam("size", "48"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.content[*].mealType", everyItem(is("LUNCH"))));

        mockMvc.perform(get("/api/v1/meal-templates")
                        .queryParam("dietaryTags", "VEGAN")
                        .queryParam("excludedAllergens", "MILK,EGG")
                        .queryParam("sort", "proteinPerServing,desc")
                        .queryParam("page", "0")
                        .queryParam("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2));
    }

    @Test
    void optionalAllergensDoNotExcludeButProduceAWarning() throws Exception {
        var rice = product("demo-mercadona-rice");
        var tuna = product("demo-mercadona-canned-tuna");
        var request = baseRequest(
                "Plantilla con pescado opcional",
                List.of(
                        ingredient(rice.getId(), "100", "GRAM", false, 0),
                        ingredient(tuna.getId(), "40", "GRAM", true, 1)
                )
        );
        create(request).andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/meal-templates")
                        .queryParam("query", "pescado opcional")
                        .queryParam("excludedAllergens", "FISH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].warnings[*]", hasItem(containsString(
                        "ingrediente opcional"
                ))));
    }

    @Test
    void createsUpdatesChangesStatusAndArchives() throws Exception {
        var rice = product("demo-mercadona-rice");
        var initial = baseRequest(
                "Plantilla CRUD de prueba",
                List.of(ingredient(rice.getId(), "100", "GRAM", false, 0))
        );
        var createdBody = create(initial)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Plantilla CRUD de prueba"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        var id = UUID.fromString(objectMapper.readTree(createdBody).get("id").asText());

        var updated = new LinkedHashMap<>(initial);
        updated.put("name", "Plantilla CRUD actualizada");
        updated.put("servings", 2);
        mockMvc.perform(put("/api/v1/meal-templates/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Plantilla CRUD actualizada"))
                .andExpect(jsonPath("$.servings").value(2));

        mockMvc.perform(patch("/api/v1/meal-templates/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(delete("/api/v1/meal-templates/{id}", id))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/meal-templates/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void previewNeverPersists() throws Exception {
        var before = mealTemplateRepository.count();
        preview(singleIngredientRequest(
                product("demo-mercadona-rice"),
                "100",
                "GRAM",
                1
        )).andExpect(status().isOk());
        org.assertj.core.api.Assertions.assertThat(mealTemplateRepository.count()).isEqualTo(before);
    }

    @Test
    void rejectsInvalidUnitsDuplicatesAndMissingRequiredIngredients() throws Exception {
        var rice = product("demo-mercadona-rice");
        preview(singleIngredientRequest(rice, "1", "UNIT", 1))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail", containsString("incompatible")));

        preview(baseRequest(
                "Duplicada",
                List.of(
                        ingredient(rice.getId(), "100", "GRAM", false, 0),
                        ingredient(rice.getId(), "50", "GRAM", false, 1)
                )
        )).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("cannot be repeated")));

        preview(baseRequest("Sin ingredientes", List.of()))
                .andExpect(status().isBadRequest());

        preview(baseRequest(
                "Solo opcionales",
                List.of(ingredient(rice.getId(), "100", "GRAM", true, 0))
        )).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("required ingredient")));
    }

    @Test
    void rejectsProductsFromAnotherSupermarket() throws Exception {
        var productId = insertCarrefourProduct();
        preview(baseRequest(
                "Supermercado incorrecto",
                List.of(ingredient(productId, "100", "GRAM", false, 0))
        )).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("another supermarket")));
    }

    @Test
    void returnsProblemDetailsForInvalidFiltersBodiesAndUnknownTemplates() throws Exception {
        mockMvc.perform(get("/api/v1/meal-templates")
                        .queryParam("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errorCode").value("BAD_REQUEST"));
        mockMvc.perform(get("/api/v1/meal-templates")
                        .queryParam("sort", "unknown,asc"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/meal-templates")
                        .queryParam("dietaryTags", "UNKNOWN"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/meal-templates/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));

        var malformed = baseRequest(
                "Estado inválido",
                List.of(ingredient(
                        product("demo-mercadona-rice").getId(),
                        "100",
                        "GRAM",
                        false,
                        0
                ))
        );
        malformed.put("mealType", "BRUNCH");
        preview(malformed)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    private org.springframework.test.web.servlet.ResultActions preview(Map<String, Object> request)
            throws Exception {
        return mockMvc.perform(post("/api/v1/meal-templates/preview")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)));
    }

    private org.springframework.test.web.servlet.ResultActions create(Map<String, Object> request)
            throws Exception {
        return mockMvc.perform(post("/api/v1/meal-templates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)));
    }

    private Map<String, Object> singleIngredientRequest(
            ProductEntity product,
            String quantity,
            String unit,
            int servings
    ) {
        var request = baseRequest(
                "Preview " + UUID.randomUUID(),
                List.of(ingredient(product.getId(), quantity, unit, false, 0))
        );
        request.put("servings", servings);
        return request;
    }

    private Map<String, Object> baseRequest(
            String name,
            List<Map<String, Object>> ingredients
    ) {
        var request = new LinkedHashMap<String, Object>();
        request.put("supermarketCode", "MERCADONA");
        request.put("name", name);
        request.put("description", "Descripción de prueba");
        request.put("mealType", "LUNCH");
        request.put("instructions", List.of("Preparar los ingredientes.", "Servir."));
        request.put("preparationMinutes", 10);
        request.put("servings", 1);
        request.put("active", true);
        request.put("ingredients", ingredients);
        return request;
    }

    private Map<String, Object> ingredient(
            UUID productId,
            String quantity,
            String unit,
            boolean optional,
            int sortOrder
    ) {
        var ingredient = new LinkedHashMap<String, Object>();
        ingredient.put("productId", productId);
        ingredient.put("quantity", new BigDecimal(quantity));
        ingredient.put("quantityUnit", unit);
        ingredient.put("optional", optional);
        ingredient.put("sortOrder", sortOrder);
        return ingredient;
    }

    private ProductEntity product(String externalId) {
        return productRepository.findAll().stream()
                .filter(product -> product.getExternalId().equals(externalId))
                .findFirst()
                .orElseThrow();
    }

    private UUID insertCarrefourProduct() {
        var supermarketId = jdbcTemplate.queryForObject(
                "SELECT id FROM supermarkets WHERE code = 'CARREFOUR'",
                UUID.class
        );
        var categoryId = UUID.randomUUID();
        var productId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO categories "
                        + "(id, supermarket_id, external_id, name, active) VALUES (?, ?, ?, ?, ?)",
                categoryId,
                supermarketId,
                "test-category-" + categoryId,
                "Categoría de prueba",
                true
        );
        var now = OffsetDateTime.now();
        jdbcTemplate.update(
                """
                INSERT INTO products (
                    id, supermarket_id, category_id, external_id, name, current_price,
                    unit_price, package_quantity, package_unit, measurement_type,
                    cost_data_complete, available, source, last_synced_at, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                productId,
                supermarketId,
                categoryId,
                "test-product-" + productId,
                "Producto Carrefour de prueba",
                new BigDecimal("1.00"),
                new BigDecimal("10.00"),
                new BigDecimal("100"),
                "G",
                "WEIGHT",
                true,
                true,
                "TEST",
                now,
                now,
                now
        );
        return productId;
    }

    private void assertThatTemplateCountIsSixteen() {
        org.assertj.core.api.Assertions.assertThat(mealTemplateRepository.count()).isEqualTo(16);
    }
}
