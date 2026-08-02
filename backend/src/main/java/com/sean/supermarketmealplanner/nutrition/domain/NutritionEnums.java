package com.sean.supermarketmealplanner.nutrition.domain;

public final class NutritionEnums {
    private NutritionEnums() {}
    public enum EnrichmentStatus { PENDING, RUNNING, SUCCESS, PARTIAL_SUCCESS, FAILED, CANCELLED;
        public boolean terminal(){return this==SUCCESS||this==PARTIAL_SUCCESS||this==FAILED||this==CANCELLED;} }
    public enum TriggeredBy { MANUAL, SCHEDULED, RETRY }
    public enum CandidateStatus { PENDING, AUTO_ACCEPTED, MANUALLY_ACCEPTED, REJECTED, EXPIRED }
    public enum MatchMethod { BARCODE_EXACT, NAME_EXACT, NAME_BRAND, FUZZY_NAME, MANUAL }
    public enum DataSource { MANUAL, LOCAL_JSON, OPEN_FOOD_FACTS, SUPERMARKET, ESTIMATED }
    public enum VerificationStatus { VERIFIED, PARTIALLY_VERIFIED, UNVERIFIED, REJECTED, MANUAL_OVERRIDE }
    public enum NutritionBasis { PER_100_GRAMS, PER_100_MILLILITERS, PER_UNIT }
}
