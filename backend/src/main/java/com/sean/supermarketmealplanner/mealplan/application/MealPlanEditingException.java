package com.sean.supermarketmealplanner.mealplan.application;

public class MealPlanEditingException extends RuntimeException {
    private final String errorCode;
    private final int status;

    public MealPlanEditingException(String message, String errorCode, int status) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }

    public String errorCode() { return errorCode; }
    public int status() { return status; }
}
