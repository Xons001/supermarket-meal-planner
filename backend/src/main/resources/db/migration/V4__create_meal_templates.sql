ALTER TABLE products
    ADD COLUMN measurement_type VARCHAR(20),
    ADD COLUMN cost_data_complete BOOLEAN NOT NULL DEFAULT TRUE;

UPDATE products
SET measurement_type = CASE
    WHEN package_unit IN ('G', 'KG') THEN 'WEIGHT'
    WHEN package_unit IN ('ML', 'L') THEN 'VOLUME'
    ELSE 'UNIT'
END;

ALTER TABLE products
    ALTER COLUMN measurement_type SET NOT NULL,
    ADD CONSTRAINT ck_products_measurement_type
        CHECK (measurement_type IN ('WEIGHT', 'VOLUME', 'UNIT'));

ALTER TABLE nutrition
    ADD COLUMN calories_per_unit NUMERIC(10, 2),
    ADD COLUMN protein_per_unit NUMERIC(10, 2),
    ADD COLUMN carbohydrates_per_unit NUMERIC(10, 2),
    ADD COLUMN fat_per_unit NUMERIC(10, 2),
    ADD COLUMN fiber_per_unit NUMERIC(10, 2),
    ADD COLUMN sugar_per_unit NUMERIC(10, 2),
    ADD COLUMN salt_per_unit NUMERIC(10, 2),
    ADD CONSTRAINT ck_nutrition_per_unit_non_negative CHECK (
        (calories_per_unit IS NULL OR calories_per_unit >= 0)
        AND (protein_per_unit IS NULL OR protein_per_unit >= 0)
        AND (carbohydrates_per_unit IS NULL OR carbohydrates_per_unit >= 0)
        AND (fat_per_unit IS NULL OR fat_per_unit >= 0)
        AND (fiber_per_unit IS NULL OR fiber_per_unit >= 0)
        AND (sugar_per_unit IS NULL OR sugar_per_unit >= 0)
        AND (salt_per_unit IS NULL OR salt_per_unit >= 0)
    );

CREATE TABLE meal_templates (
    id UUID PRIMARY KEY,
    supermarket_id UUID NOT NULL REFERENCES supermarkets(id),
    name VARCHAR(180) NOT NULL,
    description TEXT NOT NULL,
    meal_type VARCHAR(20) NOT NULL,
    preparation_minutes INTEGER NOT NULL,
    servings INTEGER NOT NULL,
    active BOOLEAN NOT NULL,
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    image_url VARCHAR(1000),
    demo_data BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_meal_templates_supermarket_name UNIQUE (supermarket_id, name),
    CONSTRAINT ck_meal_templates_meal_type
        CHECK (meal_type IN ('BREAKFAST', 'LUNCH', 'SNACK', 'DINNER')),
    CONSTRAINT ck_meal_templates_preparation_non_negative CHECK (preparation_minutes >= 0),
    CONSTRAINT ck_meal_templates_servings_positive CHECK (servings > 0)
);

CREATE TABLE meal_template_instructions (
    meal_template_id UUID NOT NULL REFERENCES meal_templates(id) ON DELETE CASCADE,
    instruction_order INTEGER NOT NULL,
    instruction TEXT NOT NULL,
    PRIMARY KEY (meal_template_id, instruction_order),
    CONSTRAINT ck_meal_template_instruction_order_non_negative CHECK (instruction_order >= 0),
    CONSTRAINT ck_meal_template_instruction_not_blank CHECK (btrim(instruction) <> '')
);

CREATE TABLE meal_template_ingredients (
    id UUID PRIMARY KEY,
    meal_template_id UUID NOT NULL REFERENCES meal_templates(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(id),
    quantity NUMERIC(12, 3) NOT NULL,
    quantity_unit VARCHAR(20) NOT NULL,
    optional BOOLEAN NOT NULL,
    sort_order INTEGER NOT NULL,
    notes VARCHAR(500),
    CONSTRAINT uq_meal_template_ingredient_product UNIQUE (meal_template_id, product_id),
    CONSTRAINT uq_meal_template_ingredient_order UNIQUE (meal_template_id, sort_order),
    CONSTRAINT ck_meal_template_ingredient_quantity_positive CHECK (quantity > 0),
    CONSTRAINT ck_meal_template_ingredient_sort_order_non_negative CHECK (sort_order >= 0),
    CONSTRAINT ck_meal_template_ingredient_unit
        CHECK (quantity_unit IN ('GRAM', 'MILLILITER', 'UNIT'))
);

CREATE INDEX idx_products_measurement_type ON products(measurement_type);
CREATE INDEX idx_meal_templates_supermarket_id ON meal_templates(supermarket_id);
CREATE INDEX idx_meal_templates_meal_type ON meal_templates(meal_type);
CREATE INDEX idx_meal_templates_active_archived ON meal_templates(active, archived);
CREATE INDEX idx_meal_templates_name_lower ON meal_templates(LOWER(name));
CREATE INDEX idx_meal_template_ingredients_template_id
    ON meal_template_ingredients(meal_template_id);
CREATE INDEX idx_meal_template_ingredients_product_id
    ON meal_template_ingredients(product_id);
