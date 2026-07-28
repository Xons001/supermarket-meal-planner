package com.sean.supermarketmealplanner.catalog;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sean.supermarketmealplanner.AbstractIntegrationTest;
import com.sean.supermarketmealplanner.catalog.infrastructure.persistence.CategoryRepository;
import com.sean.supermarketmealplanner.catalog.infrastructure.persistence.ProductRepository;
import com.sean.supermarketmealplanner.supermarket.domain.SupermarketCode;
import com.sean.supermarketmealplanner.supermarket.infrastructure.persistence.SupermarketRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class CatalogApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private SupermarketRepository supermarketRepository;

    @Test
    void listsSupermarketsWithOnlyTheDemoProviderEnabled() throws Exception {
        mockMvc.perform(get("/api/v1/supermarkets"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[0].code").value("MERCADONA"))
                .andExpect(jsonPath("$[0].enabled").value(true))
                .andExpect(jsonPath("$[1].enabled").value(false));
    }

    @Test
    void listsPaginatedDemoProductsWithControlledMetadata() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                        .queryParam("supermarketCode", "MERCADONA")
                        .queryParam("page", "0")
                        .queryParam("size", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(12)))
                .andExpect(jsonPath("$.totalElements").value(24))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content[*].demonstrationData", everyItem(org.hamcrest.Matchers.is(true))))
                .andExpect(jsonPath("$.content[0].dietaryTags").isArray())
                .andExpect(jsonPath("$.content[0].allergens").isArray());
    }

    @Test
    void searchesNameAndBrandCaseInsensitively() throws Exception {
        mockMvc.perform(get("/api/v1/products").queryParam("query", "PECHUGA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Pechuga de pollo"));

        mockMvc.perform(get("/api/v1/products").queryParam("query", "MAR DEMO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[*].brand", everyItem(org.hamcrest.Matchers.is("Mar demo"))));
    }

    @Test
    void filtersBySupermarketAndCategory() throws Exception {
        var supermarket = supermarketRepository.findByCode(SupermarketCode.MERCADONA).orElseThrow();
        var category = categoryRepository.findBySupermarketIdAndExternalId(
                supermarket.getId(),
                "demo-cat-preserves"
        ).orElseThrow();

        mockMvc.perform(get("/api/v1/products")
                        .queryParam("supermarketCode", "mercadona")
                        .queryParam("categoryId", category.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[*].categoryId",
                        everyItem(org.hamcrest.Matchers.is(category.getId().toString()))));
    }

    @Test
    void filtersByAvailabilityPriceCaloriesAndProtein() throws Exception {
        mockMvc.perform(get("/api/v1/products").queryParam("available", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[*].available",
                        everyItem(org.hamcrest.Matchers.is(false))));

        mockMvc.perform(get("/api/v1/products").queryParam("maximumPrice", "1.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].currentPrice", lessThanOrEqualTo(1.0)));

        mockMvc.perform(get("/api/v1/products").queryParam("maximumCalories", "40"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.content[*].nutrition.caloriesPer100g",
                        everyItem(lessThanOrEqualTo(40.0))));

        mockMvc.perform(get("/api/v1/products").queryParam("minimumProtein", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(6))
                .andExpect(jsonPath("$.content[*].nutrition.proteinPer100g",
                        everyItem(greaterThan(19.99))));
    }

    @Test
    void dietaryTagsRequireAllRequestedTags() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                        .queryParam("dietaryTags", "VEGAN,HIGH_PROTEIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[*].name", hasItem("Lentejas pardinas")))
                .andExpect(jsonPath("$.content[*].name", hasItem("Tofu firme")))
                .andExpect(jsonPath("$.content[*].name", hasItem("Crema de almendras")));
    }

    @Test
    void excludesProductsWithAnyRequestedAllergen() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                        .queryParam("excludedAllergens", "MILK,GLUTEN")
                        .queryParam("size", "48"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(15))
                .andExpect(jsonPath("$.content[*].allergens[*].code", not(hasItem("MILK"))))
                .andExpect(jsonPath("$.content[*].allergens[*].code", not(hasItem("GLUTEN"))));
    }

    @Test
    void combinesIndependentFilters() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                        .queryParam("available", "true")
                        .queryParam("maximumCalories", "250")
                        .queryParam("minimumProtein", "20")
                        .queryParam("dietaryTags", "HIGH_PROTEIN,LACTOSE_FREE")
                        .queryParam("excludedAllergens", "FISH,SOY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Pechuga de pollo"));
    }

    @Test
    void paginatesAndSortsWithStableMetadata() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                        .queryParam("page", "1")
                        .queryParam("size", "5")
                        .queryParam("sort", "currentPrice,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(5)))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").value(24))
                .andExpect(jsonPath("$.first").value(false))
                .andExpect(jsonPath("$.last").value(false));

        mockMvc.perform(get("/api/v1/products")
                        .queryParam("size", "5")
                        .queryParam("sort", "currentPrice,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Garbanzos cocidos"));
    }

    @Test
    void exposesCategoriesTagsAndAllergens() throws Exception {
        mockMvc.perform(get("/api/v1/categories").queryParam("supermarketCode", "MERCADONA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(8)))
                .andExpect(jsonPath("$[*].name", hasItem("Conservas")));

        mockMvc.perform(get("/api/v1/dietary-tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(7)))
                .andExpect(jsonPath("$[*].code", hasItem("LACTOSE_FREE")));

        mockMvc.perform(get("/api/v1/allergens"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(6)))
                .andExpect(jsonPath("$[*].code", hasItem("NUTS")));
    }

    @Test
    void returnsEnrichedProductDetailAndSupportsMissingNutrition() throws Exception {
        var tuna = productRepository.findAll().stream()
                .filter(product -> product.getExternalId().equals("demo-mercadona-canned-tuna"))
                .findFirst()
                .orElseThrow();
        var apple = productRepository.findAll().stream()
                .filter(product -> product.getExternalId().equals("demo-mercadona-apple"))
                .findFirst()
                .orElseThrow();

        mockMvc.perform(get("/api/v1/products/{id}", tuna.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.supermarketCode").value("MERCADONA"))
                .andExpect(jsonPath("$.categoryName").value("Conservas"))
                .andExpect(jsonPath("$.allergens[0].code").value("FISH"))
                .andExpect(jsonPath("$.dietaryTags[*].code", hasItem("HIGH_PROTEIN")));

        mockMvc.perform(get("/api/v1/products/{id}", apple.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nutrition").value(nullValue()));
    }

    @Test
    void returnsPriceHistoryNewestFirst() throws Exception {
        var chicken = productRepository.findAll().stream()
                .filter(product -> product.getExternalId().equals("demo-mercadona-chicken-breast"))
                .findFirst()
                .orElseThrow();

        mockMvc.perform(get("/api/v1/products/{id}/price-history", chicken.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].price").value(4.75))
                .andExpect(jsonPath("$[0].recordedAt").value("2026-07-28T00:00:00Z"))
                .andExpect(jsonPath("$[2].recordedAt").value("2026-05-01T00:00:00Z"))
                .andExpect(jsonPath("$[*].demonstrationData",
                        everyItem(org.hamcrest.Matchers.is(true))));
    }

    @Test
    void returnsProblemDetailsForAnUnknownProduct() throws Exception {
        var unknown = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/products/{id}", unknown))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Resource not found"))
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));

        mockMvc.perform(get("/api/v1/products/{id}/price-history", unknown))
                .andExpect(status().isNotFound());
    }

    @Test
    void validatesPageSizeSortCodesAndNumbers() throws Exception {
        assertBadRequest("page", "-1", "page must be greater");
        assertBadRequest("size", "0", "size must be between");
        assertBadRequest("size", "49", "size must be between");
        assertBadRequest("sort", "unknown,asc", "Invalid sort");
        assertBadRequest("supermarketCode", "UNKNOWN", "Invalid supermarketCode");
        assertBadRequest("dietaryTags", "NOT_A_TAG", "Invalid dietaryTags");
        assertBadRequest("excludedAllergens", "POLLEN", "Invalid excludedAllergens");
        assertBadRequest("maximumPrice", "-1", "must be non-negative");

        mockMvc.perform(get("/api/v1/products").queryParam("maximumCalories", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void validatesAnUnknownCategory() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                        .queryParam("categoryId", UUID.randomUUID().toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", org.hamcrest.Matchers.containsString("Category not found")));
    }

    private void assertBadRequest(String parameter, String value, String detail) throws Exception {
        mockMvc.perform(get("/api/v1/products").queryParam(parameter, value))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail", org.hamcrest.Matchers.containsString(detail)));
    }
}
