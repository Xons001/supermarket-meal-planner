package com.sean.supermarketmealplanner.nutrition.infrastructure.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.sean.supermarketmealplanner.nutrition.application.NutritionEnrichmentProperties;
import com.sean.supermarketmealplanner.nutrition.application.port.*;
import java.math.BigDecimal;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class OpenFoodFactsNutritionDataProvider implements NutritionDataProvider {
    private static final String FIELDS = "code,product_name,brands,quantity,product_quantity,"
            + "product_quantity_unit,categories,nutriments,last_modified_t";
    private final NutritionEnrichmentProperties properties;
    private final RestClient client;
    private final MeterRegistry metrics;
    private final AtomicLong nextRequestAt = new AtomicLong();
    private final Map<String, CachedResponse> cache = new ConcurrentHashMap<>();

    public OpenFoodFactsNutritionDataProvider(NutritionEnrichmentProperties properties, MeterRegistry metrics) {
        this.properties = properties;
        this.metrics = metrics;
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.openFoodFacts().timeoutSeconds() * 1000);
        requestFactory.setReadTimeout(properties.openFoodFacts().timeoutSeconds() * 1000);
        this.client = RestClient.builder().baseUrl(properties.openFoodFacts().baseUrl())
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.USER_AGENT, properties.openFoodFacts().userAgent()).build();
    }

    @Override public NutritionProviderCode supportedProvider() { return NutritionProviderCode.OPEN_FOOD_FACTS; }

    @Override public Optional<ExternalNutritionData> findByBarcode(String barcode) {
        if (!enabled() || barcode == null || !barcode.matches("\\d{8,14}")) return Optional.empty();
        JsonNode root = get("/api/v3/product/" + barcode + ".json?fields=" + FIELDS);
        JsonNode product = root.path("product");
        return usable(product) ? Optional.of(mapNutrition(product, barcode)) : Optional.empty();
    }

    @Override public List<ExternalNutritionCandidate> searchByName(String name, NutritionSearchOptions options) {
        if (!enabled() || name == null || name.isBlank()) return List.of();
        URI uri = UriComponentsBuilder.fromPath("/cgi/search.pl")
                .queryParam("search_terms", name).queryParam("search_simple", 1)
                .queryParam("action", "process").queryParam("json", 1)
                .queryParam("page_size", options.maximumResults()).queryParam("fields", FIELDS).build().encode().toUri();
        JsonNode root = get(uri.toString());
        var result = new ArrayList<ExternalNutritionCandidate>();
        for (JsonNode product : root.path("products")) {
            if (!usable(product)) continue;
            String code = text(product, "code");
            result.add(new ExternalNutritionCandidate("https://world.openfoodfacts.org/product/" + code,
                    code, text(product, "product_name"), text(product, "brands"), decimal(product,"product_quantity"),
                    text(product,"product_quantity_unit"), text(product,"categories"), mapNutrition(product, code)));
        }
        return result;
    }

    private JsonNode get(String path) {
        long now = System.nanoTime();
        var cached = cache.get(path);
        if (cached != null && cached.expiresAtNanos() > now) {
            metrics.counter("nutrition.provider.cache", "result", "hit", "provider", "OPEN_FOOD_FACTS").increment();
            return cached.response();
        }
        if (cached != null) cache.remove(path, cached);
        metrics.counter("nutrition.provider.cache", "result", "miss", "provider", "OPEN_FOOD_FACTS").increment();
        RuntimeException last = null;
        for (int attempt = 0; attempt <= properties.openFoodFacts().maxRetries(); attempt++) {
            throttle();
            metrics.counter("nutrition.provider.requests", "provider", "OPEN_FOOD_FACTS").increment();
            try {
                JsonNode response = client.get().uri(path).retrieve().body(JsonNode.class);
                return remember(path, response == null ? com.fasterxml.jackson.databind.node.NullNode.instance : response, false);
            }
            catch (HttpClientErrorException.NotFound ignored) {
                return remember(path, com.fasterxml.jackson.databind.node.NullNode.instance, true);
            }
            catch (HttpClientErrorException.TooManyRequests | ResourceAccessException | HttpServerErrorException ex) {
                last = ex;
            }
        }
        metrics.counter("nutrition.provider.errors", "provider", "OPEN_FOOD_FACTS").increment();
        throw new NutritionProviderUnavailableException("Open Food Facts no está disponible", last);
    }

    private JsonNode remember(String key, JsonNode response, boolean negative) {
        var ttl = negative ? properties.cacheTtl().dividedBy(4) : properties.cacheTtl();
        cache.put(key, new CachedResponse(response, System.nanoTime() + Math.max(1, ttl.toNanos())));
        return response;
    }

    private void throttle() {
        long now = System.currentTimeMillis();
        long allowed = nextRequestAt.getAndUpdate(previous -> Math.max(previous, now)
                + properties.openFoodFacts().requestDelayMs());
        long wait = allowed - now;
        if (wait > 0) try { Thread.sleep(wait); } catch (InterruptedException exception) {
            Thread.currentThread().interrupt(); throw new NutritionProviderUnavailableException("Consulta interrumpida", exception);
        }
    }
    private boolean enabled() { return properties.openFoodFacts().enabled(); }
    private boolean usable(JsonNode product) {
        JsonNode n = product.path("nutriments");
        return product.isObject() && (n.hasNonNull("energy-kcal_100g") || n.hasNonNull("energy-kj_100g"))
                && (n.hasNonNull("proteins_100g") || n.hasNonNull("fat_100g") || n.hasNonNull("carbohydrates_100g"));
    }
    private ExternalNutritionData mapNutrition(JsonNode product, String code) {
        JsonNode n = product.path("nutriments");
        BigDecimal kcal = decimal(n,"energy-kcal_100g");
        if (kcal == null && decimal(n,"energy-kj_100g") != null)
            kcal = decimal(n,"energy-kj_100g").divide(BigDecimal.valueOf(4.184),2,java.math.RoundingMode.HALF_UP);
        OffsetDateTime modified = product.hasNonNull("last_modified_t")
                ? OffsetDateTime.ofInstant(java.time.Instant.ofEpochSecond(product.get("last_modified_t").asLong()), java.time.ZoneOffset.UTC) : null;
        return new ExternalNutritionData(kcal,decimal(n,"proteins_100g"),decimal(n,"carbohydrates_100g"),
                decimal(n,"fat_100g"),decimal(n,"fiber_100g"),decimal(n,"sugars_100g"),decimal(n,"salt_100g"),
                decimal(n,"saturated-fat_100g"),"PER_100_GRAMS",null,"OPEN_FOOD_FACTS","UNVERIFIED",
                BigDecimal.ZERO,"https://world.openfoodfacts.org/product/"+code,modified);
    }
    private static String text(JsonNode node,String field){return node.hasNonNull(field)?node.get(field).asText():null;}
    private static BigDecimal decimal(JsonNode node,String field){
        if(!node.hasNonNull(field)||!node.get(field).isNumber())return null;
        return node.get(field).decimalValue();
    }
    private record CachedResponse(JsonNode response, long expiresAtNanos) {}
}
