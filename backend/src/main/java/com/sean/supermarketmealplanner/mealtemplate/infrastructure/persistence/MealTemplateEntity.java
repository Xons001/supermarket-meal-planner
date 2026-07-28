package com.sean.supermarketmealplanner.mealtemplate.infrastructure.persistence;

import com.sean.supermarketmealplanner.catalog.infrastructure.persistence.ProductEntity;
import com.sean.supermarketmealplanner.mealtemplate.domain.MealType;
import com.sean.supermarketmealplanner.mealtemplate.domain.QuantityUnit;
import com.sean.supermarketmealplanner.supermarket.infrastructure.persistence.SupermarketEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "meal_templates")
public class MealTemplateEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supermarket_id", nullable = false)
    private SupermarketEntity supermarket;

    @Column(nullable = false, length = 180)
    private String name;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "meal_type", nullable = false, length = 20)
    private MealType mealType;

    @ElementCollection
    @CollectionTable(
            name = "meal_template_instructions",
            joinColumns = @JoinColumn(name = "meal_template_id")
    )
    @OrderColumn(name = "instruction_order")
    @Column(name = "instruction", nullable = false, columnDefinition = "text")
    private List<String> instructions = new ArrayList<>();

    @Column(name = "preparation_minutes", nullable = false)
    private int preparationMinutes;

    @Column(nullable = false)
    private int servings;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private boolean archived;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    @Column(name = "demo_data", nullable = false)
    private boolean demoData;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(
            mappedBy = "mealTemplate",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("sortOrder ASC")
    private List<MealTemplateIngredientEntity> ingredients = new ArrayList<>();

    protected MealTemplateEntity() {
    }

    public MealTemplateEntity(SupermarketEntity supermarket, boolean demoData) {
        var now = OffsetDateTime.now();
        this.id = UUID.randomUUID();
        this.supermarket = supermarket;
        this.demoData = demoData;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(
            String name,
            String description,
            MealType mealType,
            List<String> instructions,
            int preparationMinutes,
            int servings,
            boolean active,
            String imageUrl
    ) {
        this.name = name;
        this.description = description;
        this.mealType = mealType;
        this.instructions.clear();
        this.instructions.addAll(instructions);
        this.preparationMinutes = preparationMinutes;
        this.servings = servings;
        this.active = active;
        this.imageUrl = imageUrl;
        this.updatedAt = OffsetDateTime.now();
    }

    public void replaceIngredients(List<IngredientData> replacements) {
        this.ingredients.clear();
        replacements.stream()
                .sorted(Comparator.comparingInt(IngredientData::sortOrder))
                .map(item -> new MealTemplateIngredientEntity(
                        this,
                        item.product(),
                        item.quantity(),
                        item.quantityUnit(),
                        item.optional(),
                        item.sortOrder(),
                        item.notes()
                ))
                .forEach(this.ingredients::add);
        this.updatedAt = OffsetDateTime.now();
    }

    public void clearCollections() {
        this.instructions.clear();
        this.ingredients.clear();
    }

    public void changeActive(boolean active) {
        this.active = active;
        this.updatedAt = OffsetDateTime.now();
    }

    public void archive() {
        this.archived = true;
        this.active = false;
        this.updatedAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public SupermarketEntity getSupermarket() {
        return supermarket;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public MealType getMealType() {
        return mealType;
    }

    public List<String> getInstructions() {
        return List.copyOf(instructions);
    }

    public int getPreparationMinutes() {
        return preparationMinutes;
    }

    public int getServings() {
        return servings;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isArchived() {
        return archived;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public boolean isDemoData() {
        return demoData;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public List<MealTemplateIngredientEntity> getIngredients() {
        return List.copyOf(ingredients);
    }

    public record IngredientData(
            ProductEntity product,
            BigDecimal quantity,
            QuantityUnit quantityUnit,
            boolean optional,
            int sortOrder,
            String notes
    ) {
    }
}
