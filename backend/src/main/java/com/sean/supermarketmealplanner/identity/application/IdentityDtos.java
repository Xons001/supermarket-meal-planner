package com.sean.supermarketmealplanner.identity.application;

import com.sean.supermarketmealplanner.mealplan.domain.GenerationStrategy;
import com.sean.supermarketmealplanner.mealplan.domain.OptimizationPreset;
import com.sean.supermarketmealplanner.identity.domain.ThemePreference;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class IdentityDtos {
    private IdentityDtos() {}
    public record RegisterRequest(@NotBlank @Email @Size(max=320) String email,
                                  @NotBlank String password,
                                  @NotBlank @Size(max=120) String displayName) {}
    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}
    public record UpdateProfileRequest(@NotBlank @Size(max=120) String displayName) {}
    public record ChangePasswordRequest(@NotBlank String currentPassword, @NotBlank String newPassword) {}
    public record DisableAccountRequest(@NotBlank String currentPassword) {}
    public record PreferencesRequest(
            @NotNull @DecimalMin("1") BigDecimal dailyCaloriesTarget,
            @NotNull @DecimalMin("1") BigDecimal dailyProteinTarget,
            @DecimalMin("0") BigDecimal weeklyBudget,
            @Min(1) @Max(7) int numberOfDays,
            @Min(1) @Max(6) int mealsPerDay,
            @NotNull GenerationStrategy strategy,
            OptimizationPreset optimizationPreset,
            List<@NotBlank String> dietaryRestrictions,
            List<@NotBlank String> allergens,
            ThemePreference theme
    ) {
        public PreferencesRequest {
            dietaryRestrictions = dietaryRestrictions == null ? List.of() : List.copyOf(dietaryRestrictions);
            allergens = allergens == null ? List.of() : List.copyOf(allergens);
            theme = theme == null ? ThemePreference.SYSTEM : theme;
        }
        public PreferencesRequest(BigDecimal dailyCaloriesTarget, BigDecimal dailyProteinTarget,
                BigDecimal weeklyBudget, int numberOfDays, int mealsPerDay, GenerationStrategy strategy,
                OptimizationPreset optimizationPreset, List<String> dietaryRestrictions, List<String> allergens) {
            this(dailyCaloriesTarget, dailyProteinTarget, weeklyBudget, numberOfDays, mealsPerDay, strategy,
                    optimizationPreset, dietaryRestrictions, allergens, ThemePreference.SYSTEM);
        }
    }
    public record PreferencesResponse(BigDecimal dailyCaloriesTarget, BigDecimal dailyProteinTarget,
            BigDecimal weeklyBudget, int numberOfDays, int mealsPerDay, GenerationStrategy strategy,
            OptimizationPreset optimizationPreset, List<String> dietaryRestrictions, List<String> allergens,
            ThemePreference theme) {
        public PreferencesResponse(BigDecimal dailyCaloriesTarget, BigDecimal dailyProteinTarget,
                BigDecimal weeklyBudget, int numberOfDays, int mealsPerDay, GenerationStrategy strategy,
                OptimizationPreset optimizationPreset, List<String> dietaryRestrictions, List<String> allergens) {
            this(dailyCaloriesTarget, dailyProteinTarget, weeklyBudget, numberOfDays, mealsPerDay, strategy,
                    optimizationPreset, dietaryRestrictions, allergens, ThemePreference.SYSTEM);
        }
    }
    public record UserResponse(UUID id, String email, String displayName, String status, String role,
                               OffsetDateTime createdAt, PreferencesResponse preferences) {}
}
