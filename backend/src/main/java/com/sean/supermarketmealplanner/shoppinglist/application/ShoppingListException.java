package com.sean.supermarketmealplanner.shoppinglist.application;

import java.util.List;
import java.util.UUID;

public class ShoppingListException extends RuntimeException {

    private final String errorCode;
    private final int status;
    private final UUID productId;
    private final String productName;
    private final List<String> unitsDetected;
    private final String expectedMeasurementType;

    public ShoppingListException(String message, String errorCode, int status) {
        this(message, errorCode, status, null, null, List.of(), null);
    }

    public ShoppingListException(
            String message,
            String errorCode,
            int status,
            UUID productId,
            String productName,
            List<String> unitsDetected,
            String expectedMeasurementType
    ) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
        this.productId = productId;
        this.productName = productName;
        this.unitsDetected = List.copyOf(unitsDetected);
        this.expectedMeasurementType = expectedMeasurementType;
    }

    public String errorCode() { return errorCode; }
    public int status() { return status; }
    public UUID productId() { return productId; }
    public String productName() { return productName; }
    public List<String> unitsDetected() { return unitsDetected; }
    public String expectedMeasurementType() { return expectedMeasurementType; }
}
