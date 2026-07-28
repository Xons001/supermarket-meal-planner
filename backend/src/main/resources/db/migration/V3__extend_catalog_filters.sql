CREATE TABLE dietary_tags (
    id UUID PRIMARY KEY,
    code VARCHAR(60) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL
);

CREATE TABLE product_dietary_tags (
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    dietary_tag_id UUID NOT NULL REFERENCES dietary_tags(id),
    PRIMARY KEY (product_id, dietary_tag_id)
);

CREATE TABLE allergens (
    id UUID PRIMARY KEY,
    code VARCHAR(60) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL
);

CREATE TABLE product_allergens (
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    allergen_id UUID NOT NULL REFERENCES allergens(id),
    presence_type VARCHAR(30) NOT NULL,
    PRIMARY KEY (product_id, allergen_id),
    CONSTRAINT ck_product_allergens_presence_type CHECK (
        presence_type IN ('CONTAINS', 'MAY_CONTAIN', 'TRACES', 'UNKNOWN')
    )
);

CREATE TABLE product_price_history (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    price NUMERIC(12, 2) NOT NULL,
    unit_price NUMERIC(12, 2) NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_product_price_history_recorded UNIQUE (product_id, recorded_at),
    CONSTRAINT ck_product_price_history_price_non_negative CHECK (price >= 0),
    CONSTRAINT ck_product_price_history_unit_price_non_negative CHECK (unit_price >= 0)
);

INSERT INTO dietary_tags (id, code, name) VALUES
    ('30000000-0000-0000-0000-000000000001', 'HIGH_PROTEIN', 'Alto en proteína'),
    ('30000000-0000-0000-0000-000000000002', 'VEGETARIAN', 'Vegetariano'),
    ('30000000-0000-0000-0000-000000000003', 'VEGAN', 'Vegano'),
    ('30000000-0000-0000-0000-000000000004', 'GLUTEN_FREE', 'Sin gluten'),
    ('30000000-0000-0000-0000-000000000005', 'LACTOSE_FREE', 'Sin lactosa'),
    ('30000000-0000-0000-0000-000000000006', 'LOW_CALORIE', 'Bajo en calorías'),
    ('30000000-0000-0000-0000-000000000007', 'HIGH_FIBER', 'Alto en fibra');

INSERT INTO allergens (id, code, name) VALUES
    ('40000000-0000-0000-0000-000000000001', 'GLUTEN', 'Gluten'),
    ('40000000-0000-0000-0000-000000000002', 'MILK', 'Leche'),
    ('40000000-0000-0000-0000-000000000003', 'EGG', 'Huevo'),
    ('40000000-0000-0000-0000-000000000004', 'FISH', 'Pescado'),
    ('40000000-0000-0000-0000-000000000005', 'SOY', 'Soja'),
    ('40000000-0000-0000-0000-000000000006', 'NUTS', 'Frutos de cáscara');

CREATE INDEX idx_products_available ON products(available);
CREATE INDEX idx_products_current_price ON products(current_price);
CREATE INDEX idx_products_name_lower ON products(LOWER(name));
CREATE INDEX idx_products_brand_lower ON products(LOWER(brand));
CREATE INDEX idx_nutrition_calories ON nutrition(calories_per_100g);
CREATE INDEX idx_nutrition_protein ON nutrition(protein_per_100g);
CREATE INDEX idx_product_dietary_tags_tag_product
    ON product_dietary_tags(dietary_tag_id, product_id);
CREATE INDEX idx_product_allergens_allergen_product
    ON product_allergens(allergen_id, product_id);
CREATE INDEX idx_product_price_history_product_recorded
    ON product_price_history(product_id, recorded_at DESC);
