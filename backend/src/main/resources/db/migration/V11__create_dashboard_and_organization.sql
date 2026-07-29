ALTER TABLE meal_plans
    ADD COLUMN favorite BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN archived_at TIMESTAMPTZ,
    ADD COLUMN duplicated_from_plan_id UUID REFERENCES meal_plans(id) ON DELETE SET NULL,
    ADD COLUMN estimated_purchase_cost NUMERIC(14,2),
    ADD COLUMN estimated_waste_cost NUMERIC(14,2),
    ADD COLUMN estimated_waste_percentage NUMERIC(6,1),
    ADD COLUMN estimated_package_count INTEGER,
    ADD COLUMN estimated_unique_product_count INTEGER;

ALTER TABLE meal_plans
    ALTER COLUMN generation_token DROP NOT NULL;

UPDATE meal_plans
SET archived_at = updated_at
WHERE archived = TRUE
  AND archived_at IS NULL;

UPDATE meal_plans
SET estimated_purchase_cost =
        NULLIF(result_json::jsonb #>> '{purchaseMetrics,estimatedPurchaseCost}', '')::NUMERIC,
    estimated_waste_cost =
        NULLIF(result_json::jsonb #>> '{purchaseMetrics,estimatedWasteCost}', '')::NUMERIC,
    estimated_waste_percentage =
        NULLIF(result_json::jsonb #>> '{purchaseMetrics,estimatedWastePercentage}', '')::NUMERIC,
    estimated_package_count =
        NULLIF(result_json::jsonb #>> '{purchaseMetrics,estimatedPackageCount}', '')::INTEGER,
    estimated_unique_product_count =
        NULLIF(result_json::jsonb #>> '{purchaseMetrics,estimatedUniqueProductCount}', '')::INTEGER
WHERE result_json::jsonb -> 'purchaseMetrics' IS NOT NULL;

ALTER TABLE shopping_lists
    ADD COLUMN active BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN archived_at TIMESTAMPTZ;

UPDATE shopping_lists
SET active = NOT archived,
    archived_at = CASE WHEN archived THEN updated_at ELSE NULL END;

DROP INDEX uq_shopping_list_active_plan;
CREATE UNIQUE INDEX uq_shopping_list_selected_plan
    ON shopping_lists(meal_plan_id)
    WHERE active = TRUE;

ALTER TABLE user_preferences
    ADD COLUMN theme VARCHAR(10) NOT NULL DEFAULT 'SYSTEM',
    ADD CONSTRAINT ck_user_preferences_theme CHECK (theme IN ('LIGHT', 'DARK', 'SYSTEM'));

ALTER TABLE planned_meals DROP CONSTRAINT ck_planned_meal_selection_source;
ALTER TABLE planned_meals
    ADD CONSTRAINT ck_planned_meal_selection_source CHECK (
        selection_source IN (
            'GENERATED',
            'MANUALLY_REPLACED',
            'PARTIALLY_REGENERATED',
            'DAY_REGENERATED',
            'DUPLICATED'
        )
    );

CREATE TABLE user_activity_events (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES user_accounts(id),
    event_type VARCHAR(40) NOT NULL,
    summary VARCHAR(500) NOT NULL,
    resource_type VARCHAR(30) NOT NULL,
    resource_id UUID NOT NULL,
    secondary_resource_id UUID,
    metadata JSONB NOT NULL DEFAULT '{}'::JSONB,
    origin VARCHAR(20) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_user_activity_origin CHECK (origin IN ('LIVE', 'BACKFILLED'))
);

-- created_at is the authoritative event time for these two creation events.
-- Phase 6 changes remain exclusively in meal_plan_changes and are not copied.
INSERT INTO user_activity_events (
    id, owner_id, event_type, summary, resource_type, resource_id,
    metadata, origin, occurred_at, created_at
)
SELECT (
           SUBSTRING(MD5('MEAL_PLAN_CREATED:' || id::TEXT), 1, 8) || '-' ||
           SUBSTRING(MD5('MEAL_PLAN_CREATED:' || id::TEXT), 9, 4) || '-' ||
           SUBSTRING(MD5('MEAL_PLAN_CREATED:' || id::TEXT), 13, 4) || '-' ||
           SUBSTRING(MD5('MEAL_PLAN_CREATED:' || id::TEXT), 17, 4) || '-' ||
           SUBSTRING(MD5('MEAL_PLAN_CREATED:' || id::TEXT), 21, 12)
       )::UUID,
       owner_id,
       'MEAL_PLAN_CREATED',
       'Plan creado',
       'MEAL_PLAN',
       id,
       JSONB_BUILD_OBJECT('name', name),
       'BACKFILLED',
       created_at,
       created_at
FROM meal_plans;

INSERT INTO user_activity_events (
    id, owner_id, event_type, summary, resource_type, resource_id,
    secondary_resource_id, metadata, origin, occurred_at, created_at
)
SELECT (
           SUBSTRING(MD5('SHOPPING_LIST_CREATED:' || id::TEXT), 1, 8) || '-' ||
           SUBSTRING(MD5('SHOPPING_LIST_CREATED:' || id::TEXT), 9, 4) || '-' ||
           SUBSTRING(MD5('SHOPPING_LIST_CREATED:' || id::TEXT), 13, 4) || '-' ||
           SUBSTRING(MD5('SHOPPING_LIST_CREATED:' || id::TEXT), 17, 4) || '-' ||
           SUBSTRING(MD5('SHOPPING_LIST_CREATED:' || id::TEXT), 21, 12)
       )::UUID,
       owner_id,
       'SHOPPING_LIST_CREATED',
       'Lista de compra creada',
       'SHOPPING_LIST',
       id,
       meal_plan_id,
       JSONB_BUILD_OBJECT('mealPlanId', meal_plan_id),
       'BACKFILLED',
       created_at,
       created_at
FROM shopping_lists;

CREATE INDEX idx_meal_plans_owner_organization
    ON meal_plans(owner_id, archived, favorite DESC, created_at DESC);
CREATE INDEX idx_meal_plans_owner_strategy
    ON meal_plans(owner_id, generation_strategy, created_at DESC);
CREATE INDEX idx_meal_plans_owner_start_date
    ON meal_plans(owner_id, start_date DESC);
CREATE INDEX idx_meal_plans_owner_purchase
    ON meal_plans(owner_id, estimated_purchase_cost);
CREATE INDEX idx_meal_plans_owner_waste
    ON meal_plans(owner_id, estimated_waste_cost);
CREATE INDEX idx_shopping_lists_owner_active
    ON shopping_lists(owner_id, active, archived, generated_at DESC);
CREATE INDEX idx_user_activity_owner_occurred
    ON user_activity_events(owner_id, occurred_at DESC, id DESC);
CREATE INDEX idx_user_activity_owner_type
    ON user_activity_events(owner_id, event_type, occurred_at DESC);
