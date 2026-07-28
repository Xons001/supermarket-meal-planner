CREATE TABLE supermarkets (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    enabled BOOLEAN NOT NULL,
    catalog_source VARCHAR(50) NOT NULL,
    country_code VARCHAR(2) NOT NULL,
    currency_code VARCHAR(3) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE categories (
    id UUID PRIMARY KEY,
    supermarket_id UUID NOT NULL REFERENCES supermarkets(id),
    external_id VARCHAR(160) NOT NULL,
    name VARCHAR(160) NOT NULL,
    parent_category_id UUID REFERENCES categories(id),
    active BOOLEAN NOT NULL,
    CONSTRAINT uq_categories_supermarket_external UNIQUE (supermarket_id, external_id)
);

CREATE TABLE products (
    id UUID PRIMARY KEY,
    supermarket_id UUID NOT NULL REFERENCES supermarkets(id),
    category_id UUID NOT NULL REFERENCES categories(id),
    external_id VARCHAR(160) NOT NULL,
    barcode VARCHAR(64),
    name VARCHAR(240) NOT NULL,
    brand VARCHAR(160),
    description TEXT,
    image_url VARCHAR(1000),
    product_url VARCHAR(1000),
    current_price NUMERIC(12, 2) NOT NULL,
    unit_price NUMERIC(12, 2) NOT NULL,
    package_quantity NUMERIC(12, 3) NOT NULL,
    package_unit VARCHAR(30) NOT NULL,
    available BOOLEAN NOT NULL,
    source VARCHAR(50) NOT NULL,
    last_synced_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_products_supermarket_external UNIQUE (supermarket_id, external_id),
    CONSTRAINT ck_products_current_price_non_negative CHECK (current_price >= 0),
    CONSTRAINT ck_products_unit_price_non_negative CHECK (unit_price >= 0),
    CONSTRAINT ck_products_package_quantity_positive CHECK (package_quantity > 0)
);

CREATE TABLE nutrition (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL UNIQUE REFERENCES products(id) ON DELETE CASCADE,
    calories_per_100g NUMERIC(10, 2) NOT NULL,
    protein_per_100g NUMERIC(10, 2) NOT NULL,
    carbohydrates_per_100g NUMERIC(10, 2) NOT NULL,
    fat_per_100g NUMERIC(10, 2) NOT NULL,
    fiber_per_100g NUMERIC(10, 2) NOT NULL,
    sugar_per_100g NUMERIC(10, 2) NOT NULL,
    salt_per_100g NUMERIC(10, 2) NOT NULL,
    data_source VARCHAR(80) NOT NULL,
    verification_status VARCHAR(40) NOT NULL,
    confidence_score NUMERIC(4, 3) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_nutrition_values_non_negative CHECK (
        calories_per_100g >= 0
        AND protein_per_100g >= 0
        AND carbohydrates_per_100g >= 0
        AND fat_per_100g >= 0
        AND fiber_per_100g >= 0
        AND sugar_per_100g >= 0
        AND salt_per_100g >= 0
    ),
    CONSTRAINT ck_nutrition_confidence_range CHECK (
        confidence_score >= 0 AND confidence_score <= 1
    )
);

CREATE INDEX idx_categories_supermarket_id ON categories(supermarket_id);
CREATE INDEX idx_categories_external_id ON categories(external_id);
CREATE INDEX idx_products_supermarket_id ON products(supermarket_id);
CREATE INDEX idx_products_category_id ON products(category_id);
CREATE INDEX idx_products_external_id ON products(external_id);
CREATE INDEX idx_products_barcode ON products(barcode);
CREATE INDEX idx_nutrition_product_id ON nutrition(product_id);
