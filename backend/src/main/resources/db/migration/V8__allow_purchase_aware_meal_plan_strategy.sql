ALTER TABLE meal_plans
    DROP CONSTRAINT ck_meal_plans_strategy;

ALTER TABLE meal_plans
    ADD CONSTRAINT ck_meal_plans_strategy CHECK (
        generation_strategy IN ('SCORING', 'PURCHASE_AWARE_SCORING')
    );
