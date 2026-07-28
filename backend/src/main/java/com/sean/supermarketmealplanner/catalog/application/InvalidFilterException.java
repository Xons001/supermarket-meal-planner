package com.sean.supermarketmealplanner.catalog.application;

public class InvalidFilterException extends RuntimeException {

    public InvalidFilterException(String message) {
        super(message);
    }
}
