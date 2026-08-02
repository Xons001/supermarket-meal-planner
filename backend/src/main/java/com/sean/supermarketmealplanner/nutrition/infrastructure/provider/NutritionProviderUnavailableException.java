package com.sean.supermarketmealplanner.nutrition.infrastructure.provider;

public class NutritionProviderUnavailableException extends RuntimeException {
    public NutritionProviderUnavailableException(String message, Throwable cause) { super(message, cause); }
}
