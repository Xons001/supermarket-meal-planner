package com.sean.supermarketmealplanner.catalog.domain;

public enum MeasurementType {
    WEIGHT,
    VOLUME,
    UNIT;

    public static MeasurementType from(PackageUnit packageUnit) {
        return switch (packageUnit) {
            case G, KG -> WEIGHT;
            case ML, L -> VOLUME;
            case UNIT -> UNIT;
        };
    }
}
