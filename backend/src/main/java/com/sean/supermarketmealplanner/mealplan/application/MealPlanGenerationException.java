package com.sean.supermarketmealplanner.mealplan.application;

import java.util.List;
import java.util.Map;

public class MealPlanGenerationException extends RuntimeException {

    private final Map<String, Integer> candidateCounts;
    private final Map<String, Integer> rejectedByReason;
    private final List<String> conflictingConstraints;
    private final List<String> suggestions;

    public MealPlanGenerationException(
            String message,
            Map<String, Integer> candidateCounts,
            Map<String, Integer> rejectedByReason,
            List<String> conflictingConstraints,
            List<String> suggestions
    ) {
        super(message);
        this.candidateCounts = Map.copyOf(candidateCounts);
        this.rejectedByReason = Map.copyOf(rejectedByReason);
        this.conflictingConstraints = List.copyOf(conflictingConstraints);
        this.suggestions = List.copyOf(suggestions);
    }

    public Map<String, Integer> getCandidateCounts() {
        return candidateCounts;
    }

    public Map<String, Integer> getRejectedByReason() {
        return rejectedByReason;
    }

    public List<String> getConflictingConstraints() {
        return conflictingConstraints;
    }

    public List<String> getSuggestions() {
        return suggestions;
    }
}
