CREATE TABLE meal_plans (
    id UUID PRIMARY KEY,
    supermarket_id UUID NOT NULL REFERENCES supermarkets(id),
    name VARCHAR(180) NOT NULL,
    start_date DATE NOT NULL,
    number_of_days INTEGER NOT NULL,
    meals_per_day INTEGER NOT NULL,
    servings INTEGER NOT NULL,
    daily_calories_target NUMERIC(12,3) NOT NULL,
    daily_protein_target NUMERIC(12,3) NOT NULL,
    weekly_budget NUMERIC(12,2),
    status VARCHAR(20) NOT NULL,
    generation_strategy VARCHAR(30) NOT NULL,
    deterministic_seed BIGINT NOT NULL,
    criteria_json TEXT NOT NULL,
    result_json TEXT NOT NULL,
    generation_token VARCHAR(64) NOT NULL,
    total_calories NUMERIC(14,3) NOT NULL,
    total_protein NUMERIC(14,3) NOT NULL,
    total_carbohydrates NUMERIC(14,3) NOT NULL,
    total_fat NUMERIC(14,3) NOT NULL,
    total_fiber NUMERIC(14,3) NOT NULL,
    total_sugar NUMERIC(14,3) NOT NULL,
    total_salt NUMERIC(14,3) NOT NULL,
    total_consumed_cost NUMERIC(14,2) NOT NULL,
    overall_score NUMERIC(6,2) NOT NULL,
    calorie_score NUMERIC(6,2) NOT NULL,
    protein_score NUMERIC(6,2) NOT NULL,
    budget_score NUMERIC(6,2) NOT NULL,
    variety_score NUMERIC(6,2) NOT NULL,
    repetition_score NUMERIC(6,2) NOT NULL,
    completeness_score NUMERIC(6,2) NOT NULL,
    preparation_score NUMERIC(6,2) NOT NULL,
    unique_templates INTEGER NOT NULL,
    repeated_templates INTEGER NOT NULL,
    maximum_observed_repetition INTEGER NOT NULL,
    calculation_complete BOOLEAN NOT NULL,
    candidates_evaluated INTEGER NOT NULL,
    complete_plans_evaluated INTEGER NOT NULL,
    duration_milliseconds BIGINT NOT NULL,
    algorithm_version VARCHAR(40) NOT NULL,
    generated_at TIMESTAMPTZ NOT NULL,
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_meal_plans_days CHECK (number_of_days BETWEEN 1 AND 14),
    CONSTRAINT ck_meal_plans_meals CHECK (meals_per_day BETWEEN 1 AND 6),
    CONSTRAINT ck_meal_plans_servings CHECK (servings > 0),
    CONSTRAINT ck_meal_plans_calories CHECK (daily_calories_target > 0),
    CONSTRAINT ck_meal_plans_protein CHECK (daily_protein_target >= 0),
    CONSTRAINT ck_meal_plans_budget CHECK (weekly_budget IS NULL OR weekly_budget > 0),
    CONSTRAINT ck_meal_plans_status CHECK (status IN ('DRAFT', 'GENERATED', 'ARCHIVED')),
    CONSTRAINT ck_meal_plans_strategy CHECK (generation_strategy = 'SCORING'),
    CONSTRAINT ck_meal_plans_score CHECK (overall_score BETWEEN 0 AND 100)
);

CREATE TABLE meal_plan_days (
    id UUID PRIMARY KEY,
    meal_plan_id UUID NOT NULL REFERENCES meal_plans(id) ON DELETE CASCADE,
    day_index INTEGER NOT NULL,
    plan_date DATE NOT NULL,
    total_calories NUMERIC(12,3) NOT NULL,
    total_protein NUMERIC(12,3) NOT NULL,
    total_carbohydrates NUMERIC(12,3) NOT NULL,
    total_fat NUMERIC(12,3) NOT NULL,
    total_fiber NUMERIC(12,3) NOT NULL,
    total_sugar NUMERIC(12,3) NOT NULL,
    total_salt NUMERIC(12,3) NOT NULL,
    total_consumed_cost NUMERIC(12,2) NOT NULL,
    calorie_deviation NUMERIC(12,3) NOT NULL,
    protein_deviation NUMERIC(12,3) NOT NULL,
    daily_score NUMERIC(6,2) NOT NULL,
    CONSTRAINT uq_meal_plan_day_index UNIQUE (meal_plan_id, day_index),
    CONSTRAINT uq_meal_plan_day_date UNIQUE (meal_plan_id, plan_date),
    CONSTRAINT ck_meal_plan_day_index CHECK (day_index >= 0)
);

CREATE TABLE planned_meals (
    id UUID PRIMARY KEY,
    meal_plan_day_id UUID NOT NULL REFERENCES meal_plan_days(id) ON DELETE CASCADE,
    meal_template_id UUID NOT NULL REFERENCES meal_templates(id),
    template_name VARCHAR(180) NOT NULL,
    meal_type VARCHAR(20) NOT NULL,
    position INTEGER NOT NULL,
    servings INTEGER NOT NULL,
    ingredients_json TEXT NOT NULL,
    calories NUMERIC(12,3) NOT NULL,
    protein NUMERIC(12,3) NOT NULL,
    carbohydrates NUMERIC(12,3) NOT NULL,
    fat NUMERIC(12,3) NOT NULL,
    fiber NUMERIC(12,3) NOT NULL,
    sugar NUMERIC(12,3) NOT NULL,
    salt NUMERIC(12,3) NOT NULL,
    consumed_cost NUMERIC(12,2) NOT NULL,
    score NUMERIC(6,2) NOT NULL,
    calculation_complete BOOLEAN NOT NULL,
    warnings_json TEXT NOT NULL,
    CONSTRAINT uq_planned_meal_position UNIQUE (meal_plan_day_id, position),
    CONSTRAINT ck_planned_meal_position CHECK (position >= 0),
    CONSTRAINT ck_planned_meal_servings CHECK (servings > 0),
    CONSTRAINT ck_planned_meal_type CHECK (
        meal_type IN ('BREAKFAST', 'LUNCH', 'SNACK', 'DINNER')
    )
);

CREATE TABLE meal_plan_warnings (
    id UUID PRIMARY KEY,
    meal_plan_id UUID NOT NULL REFERENCES meal_plans(id) ON DELETE CASCADE,
    meal_plan_day_id UUID REFERENCES meal_plan_days(id) ON DELETE CASCADE,
    warning_code VARCHAR(80) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_meal_plan_warning_severity CHECK (
        severity IN ('INFO', 'WARNING', 'ERROR')
    )
);

CREATE INDEX idx_meal_plans_supermarket_status
    ON meal_plans(supermarket_id, status)
    WHERE archived = FALSE;
CREATE INDEX idx_meal_plans_start_date ON meal_plans(start_date);
CREATE INDEX idx_meal_plans_score ON meal_plans(overall_score DESC);
CREATE INDEX idx_meal_plan_days_plan ON meal_plan_days(meal_plan_id, day_index);
CREATE INDEX idx_planned_meals_day ON planned_meals(meal_plan_day_id, position);
CREATE INDEX idx_planned_meals_template ON planned_meals(meal_template_id);
CREATE INDEX idx_meal_plan_warnings_plan ON meal_plan_warnings(meal_plan_id);
