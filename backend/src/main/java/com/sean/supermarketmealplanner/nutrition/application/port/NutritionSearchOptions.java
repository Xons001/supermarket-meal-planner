package com.sean.supermarketmealplanner.nutrition.application.port;

public record NutritionSearchOptions(String brand, String category, int maximumResults) {
    public NutritionSearchOptions {
        maximumResults = Math.min(Math.max(maximumResults, 1), 20);
    }
    public static NutritionSearchOptions defaults() { return new NutritionSearchOptions(null, null, 10); }
}
