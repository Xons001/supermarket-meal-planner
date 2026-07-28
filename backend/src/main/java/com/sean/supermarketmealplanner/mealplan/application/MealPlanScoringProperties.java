package com.sean.supermarketmealplanner.mealplan.application;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.meal-plans.scoring")
public class MealPlanScoringProperties {

    private int beamWidth = 24;
    private int candidatesPerPosition = 8;
    private BigDecimal calorieWeight = new BigDecimal("25");
    private BigDecimal proteinWeight = new BigDecimal("25");
    private BigDecimal budgetWeight = new BigDecimal("15");
    private BigDecimal varietyWeight = new BigDecimal("15");
    private BigDecimal repetitionWeight = new BigDecimal("10");
    private BigDecimal completenessWeight = new BigDecimal("5");
    private BigDecimal preparationWeight = new BigDecimal("5");
    private BigDecimal idealCalorieMarginPercentage = new BigDecimal("5");
    private BigDecimal warningCalorieMarginPercentage = new BigDecimal("10");
    private BigDecimal nutritionalDistributionShare = new BigDecimal("20");
    private BigDecimal templateVarietyShare = new BigDecimal("60");
    private BigDecimal ingredientVarietyShare = new BigDecimal("25");
    private BigDecimal mealTypeVarietyShare = new BigDecimal("15");
    private BigDecimal calorieDeviationPenaltyFactor = new BigDecimal("4");
    private BigDecimal proteinDeficitPenaltyFactor = new BigDecimal("3");
    private BigDecimal budgetExceededPenaltyFactor = new BigDecimal("4");
    private BigDecimal excessRepetitionPenalty = new BigDecimal("12");
    private BigDecimal sameDayRepetitionPenalty = new BigDecimal("20");
    private BigDecimal consecutiveRepetitionPenalty = new BigDecimal("8");
    private BigDecimal incompleteMealPenalty = new BigDecimal("20");
    private BigDecimal preparationRatioPenalty = new BigDecimal("20");
    private BigDecimal candidateCaloriePenaltyFactor = new BigDecimal("0.40");
    private BigDecimal candidateProteinPenaltyFactor = new BigDecimal("0.60");
    private BigDecimal candidateBudgetPenaltyFactor = new BigDecimal("0.30");
    private BigDecimal candidatePreparationMinutePenalty = new BigDecimal("0.15");
    private BigDecimal candidateIncompletePenalty = new BigDecimal("50");
    private BigDecimal beamExcessRepetitionPenalty = new BigDecimal("18");
    private BigDecimal beamSameDayRepetitionPenalty = new BigDecimal("25");
    private BigDecimal beamConsecutiveRepetitionPenalty = new BigDecimal("12");

    public int getBeamWidth() {
        return beamWidth;
    }

    public void setBeamWidth(int beamWidth) {
        this.beamWidth = beamWidth;
    }

    public int getCandidatesPerPosition() {
        return candidatesPerPosition;
    }

    public void setCandidatesPerPosition(int candidatesPerPosition) {
        this.candidatesPerPosition = candidatesPerPosition;
    }

    public BigDecimal getCalorieWeight() {
        return calorieWeight;
    }

    public void setCalorieWeight(BigDecimal calorieWeight) {
        this.calorieWeight = calorieWeight;
    }

    public BigDecimal getProteinWeight() {
        return proteinWeight;
    }

    public void setProteinWeight(BigDecimal proteinWeight) {
        this.proteinWeight = proteinWeight;
    }

    public BigDecimal getBudgetWeight() {
        return budgetWeight;
    }

    public void setBudgetWeight(BigDecimal budgetWeight) {
        this.budgetWeight = budgetWeight;
    }

    public BigDecimal getVarietyWeight() {
        return varietyWeight;
    }

    public void setVarietyWeight(BigDecimal varietyWeight) {
        this.varietyWeight = varietyWeight;
    }

    public BigDecimal getRepetitionWeight() {
        return repetitionWeight;
    }

    public void setRepetitionWeight(BigDecimal repetitionWeight) {
        this.repetitionWeight = repetitionWeight;
    }

    public BigDecimal getCompletenessWeight() {
        return completenessWeight;
    }

    public void setCompletenessWeight(BigDecimal completenessWeight) {
        this.completenessWeight = completenessWeight;
    }

    public BigDecimal getPreparationWeight() {
        return preparationWeight;
    }

    public void setPreparationWeight(BigDecimal preparationWeight) {
        this.preparationWeight = preparationWeight;
    }

    public BigDecimal getIdealCalorieMarginPercentage() {
        return idealCalorieMarginPercentage;
    }

    public void setIdealCalorieMarginPercentage(BigDecimal value) {
        this.idealCalorieMarginPercentage = value;
    }

    public BigDecimal getWarningCalorieMarginPercentage() {
        return warningCalorieMarginPercentage;
    }

    public void setWarningCalorieMarginPercentage(BigDecimal value) {
        this.warningCalorieMarginPercentage = value;
    }

    public BigDecimal getNutritionalDistributionShare() {
        return nutritionalDistributionShare;
    }

    public void setNutritionalDistributionShare(BigDecimal value) {
        this.nutritionalDistributionShare = value;
    }

    public BigDecimal getTemplateVarietyShare() {
        return templateVarietyShare;
    }

    public void setTemplateVarietyShare(BigDecimal value) {
        this.templateVarietyShare = value;
    }

    public BigDecimal getIngredientVarietyShare() {
        return ingredientVarietyShare;
    }

    public void setIngredientVarietyShare(BigDecimal value) {
        this.ingredientVarietyShare = value;
    }

    public BigDecimal getMealTypeVarietyShare() {
        return mealTypeVarietyShare;
    }

    public void setMealTypeVarietyShare(BigDecimal value) {
        this.mealTypeVarietyShare = value;
    }

    public BigDecimal totalWeight() {
        return calorieWeight.add(proteinWeight).add(budgetWeight).add(varietyWeight)
                .add(repetitionWeight).add(completenessWeight).add(preparationWeight);
    }

    public BigDecimal getCalorieDeviationPenaltyFactor() {
        return calorieDeviationPenaltyFactor;
    }

    public void setCalorieDeviationPenaltyFactor(BigDecimal value) {
        this.calorieDeviationPenaltyFactor = value;
    }

    public BigDecimal getProteinDeficitPenaltyFactor() {
        return proteinDeficitPenaltyFactor;
    }

    public void setProteinDeficitPenaltyFactor(BigDecimal value) {
        this.proteinDeficitPenaltyFactor = value;
    }

    public BigDecimal getBudgetExceededPenaltyFactor() {
        return budgetExceededPenaltyFactor;
    }

    public void setBudgetExceededPenaltyFactor(BigDecimal value) {
        this.budgetExceededPenaltyFactor = value;
    }

    public BigDecimal getExcessRepetitionPenalty() {
        return excessRepetitionPenalty;
    }

    public void setExcessRepetitionPenalty(BigDecimal value) {
        this.excessRepetitionPenalty = value;
    }

    public BigDecimal getSameDayRepetitionPenalty() {
        return sameDayRepetitionPenalty;
    }

    public void setSameDayRepetitionPenalty(BigDecimal value) {
        this.sameDayRepetitionPenalty = value;
    }

    public BigDecimal getConsecutiveRepetitionPenalty() {
        return consecutiveRepetitionPenalty;
    }

    public void setConsecutiveRepetitionPenalty(BigDecimal value) {
        this.consecutiveRepetitionPenalty = value;
    }

    public BigDecimal getIncompleteMealPenalty() {
        return incompleteMealPenalty;
    }

    public void setIncompleteMealPenalty(BigDecimal value) {
        this.incompleteMealPenalty = value;
    }

    public BigDecimal getPreparationRatioPenalty() {
        return preparationRatioPenalty;
    }

    public void setPreparationRatioPenalty(BigDecimal value) {
        this.preparationRatioPenalty = value;
    }

    public BigDecimal getCandidateCaloriePenaltyFactor() {
        return candidateCaloriePenaltyFactor;
    }

    public void setCandidateCaloriePenaltyFactor(BigDecimal value) {
        this.candidateCaloriePenaltyFactor = value;
    }

    public BigDecimal getCandidateProteinPenaltyFactor() {
        return candidateProteinPenaltyFactor;
    }

    public void setCandidateProteinPenaltyFactor(BigDecimal value) {
        this.candidateProteinPenaltyFactor = value;
    }

    public BigDecimal getCandidateBudgetPenaltyFactor() {
        return candidateBudgetPenaltyFactor;
    }

    public void setCandidateBudgetPenaltyFactor(BigDecimal value) {
        this.candidateBudgetPenaltyFactor = value;
    }

    public BigDecimal getCandidatePreparationMinutePenalty() {
        return candidatePreparationMinutePenalty;
    }

    public void setCandidatePreparationMinutePenalty(BigDecimal value) {
        this.candidatePreparationMinutePenalty = value;
    }

    public BigDecimal getCandidateIncompletePenalty() {
        return candidateIncompletePenalty;
    }

    public void setCandidateIncompletePenalty(BigDecimal value) {
        this.candidateIncompletePenalty = value;
    }

    public BigDecimal getBeamExcessRepetitionPenalty() {
        return beamExcessRepetitionPenalty;
    }

    public void setBeamExcessRepetitionPenalty(BigDecimal value) {
        this.beamExcessRepetitionPenalty = value;
    }

    public BigDecimal getBeamSameDayRepetitionPenalty() {
        return beamSameDayRepetitionPenalty;
    }

    public void setBeamSameDayRepetitionPenalty(BigDecimal value) {
        this.beamSameDayRepetitionPenalty = value;
    }

    public BigDecimal getBeamConsecutiveRepetitionPenalty() {
        return beamConsecutiveRepetitionPenalty;
    }

    public void setBeamConsecutiveRepetitionPenalty(BigDecimal value) {
        this.beamConsecutiveRepetitionPenalty = value;
    }
}
