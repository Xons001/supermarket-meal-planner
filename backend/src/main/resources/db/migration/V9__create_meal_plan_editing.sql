ALTER TABLE meal_plans
    ADD COLUMN edit_version BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN content_version BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN row_version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE planned_meals
    ADD COLUMN locked BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN selection_source VARCHAR(30) NOT NULL DEFAULT 'GENERATED',
    ADD COLUMN edit_version BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN modified_at TIMESTAMPTZ,
    ADD COLUMN original_meal_template_id UUID,
    ADD COLUMN partial_generation_seed BIGINT,
    ADD CONSTRAINT ck_planned_meal_selection_source CHECK (
        selection_source IN (
            'GENERATED',
            'MANUALLY_REPLACED',
            'PARTIALLY_REGENERATED',
            'DAY_REGENERATED'
        )
    );

ALTER TABLE shopping_lists
    ADD COLUMN source_plan_content_version BIGINT NOT NULL DEFAULT 0;

CREATE TABLE meal_plan_changes (
    id UUID PRIMARY KEY,
    meal_plan_id UUID NOT NULL REFERENCES meal_plans(id) ON DELETE CASCADE,
    sequence_number BIGINT NOT NULL,
    change_type VARCHAR(30) NOT NULL,
    edit_version_before BIGINT NOT NULL,
    edit_version_after BIGINT NOT NULL,
    content_version_before BIGINT NOT NULL,
    content_version_after BIGINT NOT NULL,
    meal_plan_day_id UUID REFERENCES meal_plan_days(id) ON DELETE SET NULL,
    planned_meal_id UUID REFERENCES planned_meals(id) ON DELETE SET NULL,
    before_snapshot JSONB NOT NULL,
    after_snapshot JSONB NOT NULL,
    metrics_before JSONB NOT NULL,
    metrics_after JSONB NOT NULL,
    metrics_delta JSONB NOT NULL,
    deterministic_seed BIGINT,
    generation_strategy VARCHAR(30) NOT NULL,
    optimization_preset VARCHAR(30),
    reason VARCHAR(500) NOT NULL,
    undone_by_change_id UUID REFERENCES meal_plan_changes(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_meal_plan_change_sequence UNIQUE (meal_plan_id, sequence_number),
    CONSTRAINT ck_meal_plan_change_type CHECK (
        change_type IN (
            'MEAL_REPLACED',
            'MEAL_REGENERATED',
            'DAY_REGENERATED',
            'MEAL_LOCKED',
            'MEAL_UNLOCKED',
            'CHANGE_UNDONE'
        )
    )
);

CREATE INDEX idx_meal_plan_changes_plan
    ON meal_plan_changes(meal_plan_id, sequence_number DESC);
CREATE INDEX idx_meal_plan_changes_undo
    ON meal_plan_changes(meal_plan_id, undone_by_change_id);
CREATE INDEX idx_shopping_lists_plan_version
    ON shopping_lists(meal_plan_id, source_plan_content_version);
