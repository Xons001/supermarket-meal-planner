-- FASE 10: nutrition enrichment, traceability and deterministic review workflow.
ALTER TABLE nutrition ALTER COLUMN calories_per_100g DROP NOT NULL;
ALTER TABLE nutrition ALTER COLUMN protein_per_100g DROP NOT NULL;
ALTER TABLE nutrition ALTER COLUMN carbohydrates_per_100g DROP NOT NULL;
ALTER TABLE nutrition ALTER COLUMN fat_per_100g DROP NOT NULL;
ALTER TABLE nutrition ALTER COLUMN fiber_per_100g DROP NOT NULL;
ALTER TABLE nutrition ALTER COLUMN sugar_per_100g DROP NOT NULL;
ALTER TABLE nutrition ALTER COLUMN salt_per_100g DROP NOT NULL;
ALTER TABLE nutrition ALTER COLUMN confidence_score TYPE NUMERIC(5,2);
ALTER TABLE nutrition DROP CONSTRAINT ck_nutrition_confidence_range;
UPDATE nutrition SET confidence_score = confidence_score * 100 WHERE confidence_score <= 1;
ALTER TABLE nutrition ADD CONSTRAINT ck_nutrition_confidence_range
    CHECK (confidence_score >= 0 AND confidence_score <= 100);

ALTER TABLE nutrition
    ADD COLUMN saturated_fat_per_100g NUMERIC(10,2),
    ADD COLUMN nutrition_basis VARCHAR(40) NOT NULL DEFAULT 'PER_100_GRAMS',
    ADD COLUMN completeness VARCHAR(20) NOT NULL DEFAULT 'COMPLETE',
    ADD COLUMN source_reference VARCHAR(500),
    ADD COLUMN source_updated_at TIMESTAMPTZ,
    ADD COLUMN reviewed_by UUID REFERENCES user_accounts(id),
    ADD COLUMN reviewed_at TIMESTAMPTZ,
    ADD COLUMN created_at TIMESTAMPTZ,
    ADD COLUMN row_version BIGINT NOT NULL DEFAULT 0;

UPDATE nutrition SET created_at = updated_at WHERE created_at IS NULL;
ALTER TABLE nutrition ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE nutrition ADD CONSTRAINT ck_nutrition_basis
    CHECK (nutrition_basis IN ('PER_100_GRAMS','PER_100_MILLILITERS','PER_UNIT'));
ALTER TABLE nutrition ADD CONSTRAINT ck_nutrition_completeness
    CHECK (completeness IN ('COMPLETE','PARTIAL','MINIMAL','EMPTY'));
ALTER TABLE nutrition ADD CONSTRAINT ck_nutrition_saturated_fat
    CHECK (saturated_fat_per_100g IS NULL OR saturated_fat_per_100g >= 0);

CREATE TABLE nutrition_enrichment_runs (
    id UUID PRIMARY KEY,
    provider VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL,
    triggered_by VARCHAR(30) NOT NULL,
    requested_by UUID REFERENCES user_accounts(id),
    started_at TIMESTAMPTZ,
    finished_at TIMESTAMPTZ,
    products_scanned INTEGER NOT NULL DEFAULT 0,
    barcode_matches INTEGER NOT NULL DEFAULT 0,
    name_matches INTEGER NOT NULL DEFAULT 0,
    auto_accepted INTEGER NOT NULL DEFAULT 0,
    pending_review INTEGER NOT NULL DEFAULT 0,
    rejected INTEGER NOT NULL DEFAULT 0,
    updated_products INTEGER NOT NULL DEFAULT 0,
    unchanged_products INTEGER NOT NULL DEFAULT 0,
    errors INTEGER NOT NULL DEFAULT 0,
    duration_ms BIGINT,
    report_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    airflow_dag_run_id VARCHAR(250) UNIQUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_nutrition_enrichment_status CHECK
        (status IN ('PENDING','RUNNING','SUCCESS','PARTIAL_SUCCESS','FAILED','CANCELLED')),
    CONSTRAINT ck_nutrition_enrichment_trigger CHECK
        (triggered_by IN ('MANUAL','SCHEDULED','RETRY')),
    CONSTRAINT ck_nutrition_enrichment_counts CHECK
        (products_scanned >= 0 AND barcode_matches >= 0 AND name_matches >= 0
         AND auto_accepted >= 0 AND pending_review >= 0 AND rejected >= 0
         AND updated_products >= 0 AND unchanged_products >= 0 AND errors >= 0)
);

CREATE UNIQUE INDEX uq_nutrition_enrichment_active
    ON nutrition_enrichment_runs ((true)) WHERE status IN ('PENDING','RUNNING');
CREATE INDEX idx_nutrition_enrichment_runs_created
    ON nutrition_enrichment_runs (created_at DESC);
CREATE INDEX idx_nutrition_enrichment_runs_status
    ON nutrition_enrichment_runs (status, created_at DESC);

CREATE TABLE nutrition_enrichment_errors (
    id UUID PRIMARY KEY,
    run_id UUID NOT NULL REFERENCES nutrition_enrichment_runs(id) ON DELETE CASCADE,
    product_id UUID REFERENCES products(id) ON DELETE SET NULL,
    code VARCHAR(80) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    retryable BOOLEAN NOT NULL DEFAULT FALSE,
    error_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_nutrition_enrichment_error UNIQUE (run_id, error_hash)
);

CREATE TABLE nutrition_match_candidates (
    id UUID PRIMARY KEY,
    run_id UUID REFERENCES nutrition_enrichment_runs(id) ON DELETE SET NULL,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    provider VARCHAR(50) NOT NULL,
    external_reference VARCHAR(500) NOT NULL,
    external_barcode VARCHAR(64),
    external_name VARCHAR(300) NOT NULL,
    normalized_name VARCHAR(300) NOT NULL,
    brand VARCHAR(200),
    candidate_payload_json JSONB NOT NULL,
    match_method VARCHAR(30) NOT NULL,
    confidence_score NUMERIC(5,2) NOT NULL,
    score_breakdown_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    status VARCHAR(30) NOT NULL,
    rejection_reason VARCHAR(1000),
    source_hash VARCHAR(64) NOT NULL,
    source_updated_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ NOT NULL,
    reviewed_at TIMESTAMPTZ,
    reviewed_by UUID REFERENCES user_accounts(id),
    created_at TIMESTAMPTZ NOT NULL,
    row_version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_nutrition_candidate_method CHECK
        (match_method IN ('BARCODE_EXACT','NAME_EXACT','NAME_BRAND','FUZZY_NAME','MANUAL')),
    CONSTRAINT ck_nutrition_candidate_status CHECK
        (status IN ('PENDING','AUTO_ACCEPTED','MANUALLY_ACCEPTED','REJECTED','EXPIRED')),
    CONSTRAINT ck_nutrition_candidate_confidence CHECK
        (confidence_score >= 0 AND confidence_score <= 100),
    CONSTRAINT uq_nutrition_candidate_source UNIQUE (product_id, provider, external_reference, source_hash)
);
CREATE INDEX idx_nutrition_candidates_review
    ON nutrition_match_candidates (status, confidence_score DESC, created_at ASC);
CREATE INDEX idx_nutrition_candidates_product
    ON nutrition_match_candidates (product_id, created_at DESC);
CREATE INDEX idx_nutrition_candidates_expiry
    ON nutrition_match_candidates (expires_at) WHERE status = 'PENDING';

CREATE TABLE product_nutrition_history (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    previous_snapshot_json JSONB,
    new_snapshot_json JSONB NOT NULL,
    change_source VARCHAR(30) NOT NULL,
    provider VARCHAR(50),
    confidence_score NUMERIC(5,2),
    changed_by UUID REFERENCES user_accounts(id),
    changed_at TIMESTAMPTZ NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    snapshot_hash VARCHAR(64) NOT NULL,
    CONSTRAINT ck_nutrition_history_source CHECK
        (change_source IN ('AUTOMATIC','MANUAL','CANDIDATE_ACCEPTED'))
);
CREATE UNIQUE INDEX uq_nutrition_history_snapshot
    ON product_nutrition_history (product_id, snapshot_hash);
CREATE INDEX idx_nutrition_history_product
    ON product_nutrition_history (product_id, changed_at DESC);

CREATE TABLE nutrition_provider_cache (
    provider VARCHAR(50) NOT NULL,
    lookup_key VARCHAR(500) NOT NULL,
    response_hash VARCHAR(64),
    status VARCHAR(30) NOT NULL,
    response_json JSONB,
    cached_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (provider, lookup_key),
    CONSTRAINT ck_nutrition_cache_status CHECK
        (status IN ('FOUND','NOT_FOUND','TEMPORARY_ERROR'))
);
CREATE INDEX idx_nutrition_cache_expiry ON nutrition_provider_cache (expires_at);
