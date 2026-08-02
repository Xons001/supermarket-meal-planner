package com.sean.supermarketmealplanner.nutrition.application;

import org.springframework.http.HttpStatus;

public class NutritionException extends RuntimeException {
    private final HttpStatus status; private final String code;
    public NutritionException(HttpStatus status,String code,String message){super(message);this.status=status;this.code=code;}
    public HttpStatus status(){return status;} public String code(){return code;}
}
