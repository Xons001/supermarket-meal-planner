package com.sean.supermarketmealplanner.mealtemplate.domain;

import com.sean.supermarketmealplanner.catalog.domain.MeasurementType;

public enum QuantityUnit {
    GRAM(MeasurementType.WEIGHT),
    MILLILITER(MeasurementType.VOLUME),
    UNIT(MeasurementType.UNIT);

    private final MeasurementType measurementType;

    QuantityUnit(MeasurementType measurementType) {
        this.measurementType = measurementType;
    }

    public boolean isCompatibleWith(MeasurementType productMeasurementType) {
        return measurementType == productMeasurementType;
    }
}
