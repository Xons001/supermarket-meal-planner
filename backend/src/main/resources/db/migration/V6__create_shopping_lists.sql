CREATE TABLE shopping_lists (
    id UUID PRIMARY KEY,
    meal_plan_id UUID NOT NULL REFERENCES meal_plans(id),
    supermarket_id UUID NOT NULL REFERENCES supermarkets(id),
    status VARCHAR(20) NOT NULL,
    total_packages INTEGER NOT NULL,
    total_consumed_cost NUMERIC(14,2) NOT NULL,
    total_purchase_cost NUMERIC(14,2) NOT NULL,
    total_waste_cost NUMERIC(14,2) NOT NULL,
    overall_waste_percentage NUMERIC(6,1) NOT NULL,
    quantity_summary_json TEXT NOT NULL,
    weekly_budget NUMERIC(14,2),
    purchase_budget_difference NUMERIC(14,2),
    purchase_budget_exceeded BOOLEAN NOT NULL,
    budget_calculation_complete BOOLEAN NOT NULL,
    calculation_complete BOOLEAN NOT NULL,
    generation_duration_milliseconds BIGINT NOT NULL,
    demo_data BOOLEAN NOT NULL,
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    generated_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_shopping_list_status CHECK (status IN ('GENERATED', 'ARCHIVED')),
    CONSTRAINT ck_shopping_list_packages CHECK (total_packages >= 0),
    CONSTRAINT ck_shopping_list_costs CHECK (
        total_consumed_cost >= 0
        AND total_purchase_cost >= 0
        AND total_waste_cost >= 0
    ),
    CONSTRAINT ck_shopping_list_waste_percentage CHECK (
        overall_waste_percentage BETWEEN 0 AND 100
    ),
    CONSTRAINT ck_shopping_list_status_archive CHECK (
        (status = 'ARCHIVED' AND archived = TRUE)
        OR (status = 'GENERATED' AND archived = FALSE)
    )
);

CREATE TABLE shopping_list_items (
    id UUID PRIMARY KEY,
    shopping_list_id UUID NOT NULL REFERENCES shopping_lists(id) ON DELETE CASCADE,
    product_id UUID NOT NULL,
    category_id UUID,
    product_name_snapshot VARCHAR(240) NOT NULL,
    brand_snapshot VARCHAR(160),
    category_name_snapshot VARCHAR(160),
    measurement_type VARCHAR(20),
    required_unit VARCHAR(30) NOT NULL,
    package_quantity_snapshot NUMERIC(14,3),
    package_unit_snapshot VARCHAR(30),
    package_price_snapshot NUMERIC(14,2),
    unit_price_snapshot NUMERIC(14,2),
    required_quantity NUMERIC(14,3) NOT NULL,
    packages_required INTEGER,
    purchased_quantity NUMERIC(14,3),
    leftover_quantity NUMERIC(14,3),
    consumed_cost NUMERIC(14,2),
    purchase_cost NUMERIC(14,2),
    waste_cost NUMERIC(14,2),
    leftover_percentage NUMERIC(6,1),
    available_snapshot BOOLEAN NOT NULL,
    calculation_complete BOOLEAN NOT NULL,
    sort_order INTEGER NOT NULL,
    warnings_snapshot TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_shopping_list_product UNIQUE (shopping_list_id, product_id),
    CONSTRAINT ck_shopping_item_measurement CHECK (
        measurement_type IS NULL OR measurement_type IN ('WEIGHT', 'VOLUME', 'UNIT')
    ),
    CONSTRAINT ck_shopping_item_required CHECK (required_quantity >= 0),
    CONSTRAINT ck_shopping_item_packages CHECK (
        packages_required IS NULL OR packages_required >= 0
    ),
    CONSTRAINT ck_shopping_item_complete_values CHECK (
        calculation_complete = FALSE
        OR (
            packages_required IS NOT NULL
            AND purchased_quantity IS NOT NULL
            AND purchased_quantity >= required_quantity
            AND leftover_quantity = purchased_quantity - required_quantity
            AND purchase_cost IS NOT NULL
            AND waste_cost IS NOT NULL
        )
    ),
    CONSTRAINT ck_shopping_item_leftover_percentage CHECK (
        leftover_percentage IS NULL OR leftover_percentage BETWEEN 0 AND 100
    ),
    CONSTRAINT ck_shopping_item_sort_order CHECK (sort_order >= 0)
);

CREATE TABLE shopping_list_warnings (
    id UUID PRIMARY KEY,
    shopping_list_id UUID NOT NULL REFERENCES shopping_lists(id) ON DELETE CASCADE,
    shopping_list_item_id UUID REFERENCES shopping_list_items(id) ON DELETE CASCADE,
    warning_code VARCHAR(80) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_shopping_list_warning_severity CHECK (
        severity IN ('INFO', 'WARNING', 'ERROR')
    )
);

CREATE UNIQUE INDEX uq_shopping_list_active_plan
    ON shopping_lists(meal_plan_id)
    WHERE archived = FALSE;
CREATE INDEX idx_shopping_lists_supermarket_status
    ON shopping_lists(supermarket_id, status);
CREATE INDEX idx_shopping_lists_generated_at
    ON shopping_lists(generated_at DESC);
CREATE INDEX idx_shopping_lists_purchase_cost
    ON shopping_lists(total_purchase_cost DESC);
CREATE INDEX idx_shopping_list_items_list
    ON shopping_list_items(shopping_list_id, sort_order);
CREATE INDEX idx_shopping_list_items_product
    ON shopping_list_items(product_id);
CREATE INDEX idx_shopping_list_warnings_list
    ON shopping_list_warnings(shopping_list_id);
