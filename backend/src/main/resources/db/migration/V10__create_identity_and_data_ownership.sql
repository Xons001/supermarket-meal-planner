CREATE TABLE user_accounts (
    id UUID PRIMARY KEY,
    normalized_email VARCHAR(320) NOT NULL UNIQUE,
    password_hash VARCHAR(255),
    display_name VARCHAR(120) NOT NULL,
    status VARCHAR(16) NOT NULL CHECK (status IN ('ACTIVE', 'DISABLED')),
    role VARCHAR(16) NOT NULL CHECK (role IN ('USER', 'ADMIN')),
    last_login_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    row_version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT usable_password_for_active_account
        CHECK (status = 'DISABLED' OR password_hash IS NOT NULL)
);

CREATE TABLE user_preferences (
    user_id UUID PRIMARY KEY REFERENCES user_accounts(id) ON DELETE CASCADE,
    daily_calories_target NUMERIC(12,3) NOT NULL DEFAULT 2000,
    daily_protein_target NUMERIC(12,3) NOT NULL DEFAULT 100,
    weekly_budget NUMERIC(12,2) DEFAULT 70,
    number_of_days INTEGER NOT NULL DEFAULT 7 CHECK (number_of_days BETWEEN 1 AND 7),
    meals_per_day INTEGER NOT NULL DEFAULT 4 CHECK (meals_per_day BETWEEN 1 AND 6),
    generation_strategy VARCHAR(30) NOT NULL DEFAULT 'PURCHASE_AWARE_SCORING',
    optimization_preset VARCHAR(30) DEFAULT 'BALANCED',
    dietary_restrictions JSONB NOT NULL DEFAULT '[]'::jsonb,
    allergens JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE refresh_token_sessions (
    id UUID PRIMARY KEY,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    family_id UUID NOT NULL,
    user_id UUID NOT NULL REFERENCES user_accounts(id) ON DELETE CASCADE,
    expires_at TIMESTAMPTZ NOT NULL,
    rotated_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    replaced_by UUID REFERENCES refresh_token_sessions(id),
    user_agent VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_refresh_sessions_user_active
    ON refresh_token_sessions(user_id, revoked_at, expires_at);
CREATE INDEX idx_refresh_sessions_family ON refresh_token_sessions(family_id);

INSERT INTO user_accounts (
    id, normalized_email, password_hash, display_name, status, role, created_at, updated_at
) VALUES (
    '00000000-0000-4000-8000-000000000007',
    'historical-owner@invalid.local',
    NULL,
    'Propietario técnico histórico',
    'DISABLED',
    'USER',
    NOW(),
    NOW()
);

INSERT INTO user_preferences (user_id, created_at, updated_at)
VALUES ('00000000-0000-4000-8000-000000000007', NOW(), NOW());

ALTER TABLE meal_plans ADD COLUMN owner_id UUID;
UPDATE meal_plans
SET owner_id = '00000000-0000-4000-8000-000000000007'
WHERE owner_id IS NULL;
ALTER TABLE meal_plans ALTER COLUMN owner_id SET NOT NULL;
ALTER TABLE meal_plans
    ADD CONSTRAINT fk_meal_plan_owner FOREIGN KEY (owner_id) REFERENCES user_accounts(id);
ALTER TABLE meal_plans ADD CONSTRAINT uq_meal_plan_id_owner UNIQUE (id, owner_id);
CREATE INDEX idx_meal_plans_owner_created ON meal_plans(owner_id, created_at DESC);

ALTER TABLE shopping_lists ADD COLUMN owner_id UUID;
UPDATE shopping_lists sl
SET owner_id = mp.owner_id
FROM meal_plans mp
WHERE sl.meal_plan_id = mp.id;
ALTER TABLE shopping_lists ALTER COLUMN owner_id SET NOT NULL;
ALTER TABLE shopping_lists
    ADD CONSTRAINT fk_shopping_list_owner FOREIGN KEY (owner_id) REFERENCES user_accounts(id);
ALTER TABLE shopping_lists
    ADD CONSTRAINT fk_shopping_list_plan_owner
    FOREIGN KEY (meal_plan_id, owner_id) REFERENCES meal_plans(id, owner_id)
    DEFERRABLE INITIALLY IMMEDIATE;
CREATE INDEX idx_shopping_lists_owner_created ON shopping_lists(owner_id, created_at DESC);
