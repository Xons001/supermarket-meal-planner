ALTER TABLE products ADD COLUMN last_seen_at TIMESTAMPTZ;
ALTER TABLE products ADD COLUMN unavailable_since TIMESTAMPTZ;
UPDATE products SET last_seen_at = last_synced_at WHERE last_seen_at IS NULL;

CREATE TABLE catalog_sync_runs (
    id UUID PRIMARY KEY,
    supermarket_id UUID NOT NULL REFERENCES supermarkets(id),
    sync_type VARCHAR(30) NOT NULL,
    triggered_by VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL,
    provider VARCHAR(50) NOT NULL,
    airflow_dag_id VARCHAR(120),
    airflow_dag_run_id VARCHAR(200) UNIQUE,
    requested_by_user_id UUID REFERENCES user_accounts(id) ON DELETE SET NULL,
    retry_of_sync_run_id UUID REFERENCES catalog_sync_runs(id),
    categories_processed INTEGER NOT NULL DEFAULT 0,
    products_processed INTEGER NOT NULL DEFAULT 0,
    products_created INTEGER NOT NULL DEFAULT 0,
    products_updated INTEGER NOT NULL DEFAULT 0,
    products_unavailable INTEGER NOT NULL DEFAULT 0,
    prices_changed INTEGER NOT NULL DEFAULT 0,
    validation_errors INTEGER NOT NULL DEFAULT 0,
    configuration_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    result_json JSONB,
    requested_at TIMESTAMPTZ NOT NULL,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_catalog_sync_type CHECK (sync_type IN ('FULL_CATALOG', 'PRICES_ONLY')),
    CONSTRAINT ck_catalog_sync_trigger CHECK (triggered_by IN ('MANUAL', 'SCHEDULED', 'RETRY')),
    CONSTRAINT ck_catalog_sync_status CHECK (status IN ('PENDING','RUNNING','SUCCESS','PARTIAL_SUCCESS','FAILED')),
    CONSTRAINT ck_catalog_sync_counts CHECK (
        categories_processed >= 0 AND products_processed >= 0 AND products_created >= 0
        AND products_updated >= 0 AND products_unavailable >= 0 AND prices_changed >= 0
        AND validation_errors >= 0
    )
);

CREATE UNIQUE INDEX uq_catalog_sync_running_supermarket
    ON catalog_sync_runs(supermarket_id)
    WHERE status IN ('PENDING', 'RUNNING');
CREATE INDEX idx_catalog_sync_runs_search
    ON catalog_sync_runs(supermarket_id, status, sync_type, requested_at DESC);

CREATE TABLE catalog_sync_errors (
    id UUID PRIMARY KEY,
    sync_run_id UUID NOT NULL REFERENCES catalog_sync_runs(id) ON DELETE CASCADE,
    severity VARCHAR(12) NOT NULL,
    entity_type VARCHAR(40) NOT NULL,
    external_id VARCHAR(160),
    error_code VARCHAR(80) NOT NULL,
    message TEXT NOT NULL,
    raw_data_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_catalog_sync_error_severity CHECK (severity IN ('WARNING','ERROR','FATAL')),
    CONSTRAINT uq_catalog_sync_error UNIQUE (sync_run_id, entity_type, external_id, error_code, raw_data_hash)
);
CREATE INDEX idx_catalog_sync_errors_run_created ON catalog_sync_errors(sync_run_id, created_at, id);

CREATE TABLE staging_categories (
    id BIGSERIAL PRIMARY KEY,
    sync_run_id UUID NOT NULL REFERENCES catalog_sync_runs(id) ON DELETE CASCADE,
    external_id VARCHAR(160) NOT NULL,
    name VARCHAR(160),
    parent_external_id VARCHAR(160),
    valid BOOLEAN NOT NULL,
    raw_data_hash VARCHAR(64) NOT NULL,
    raw_data JSONB NOT NULL,
    observed_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_staging_category UNIQUE(sync_run_id, external_id)
);

CREATE TABLE staging_products (
    id BIGSERIAL PRIMARY KEY,
    sync_run_id UUID NOT NULL REFERENCES catalog_sync_runs(id) ON DELETE CASCADE,
    external_id VARCHAR(160) NOT NULL,
    category_external_id VARCHAR(160),
    barcode VARCHAR(64), name VARCHAR(240), brand VARCHAR(160), description TEXT,
    image_url VARCHAR(1000), product_url VARCHAR(1000),
    current_price NUMERIC(12,2), unit_price NUMERIC(12,2), package_quantity NUMERIC(12,3),
    package_unit VARCHAR(30), measurement_type VARCHAR(20), cost_data_complete BOOLEAN,
    available BOOLEAN, source VARCHAR(50), valid BOOLEAN NOT NULL,
    raw_data_hash VARCHAR(64) NOT NULL, raw_data JSONB NOT NULL, observed_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_staging_product UNIQUE(sync_run_id, external_id)
);

CREATE TABLE staging_prices (
    id BIGSERIAL PRIMARY KEY,
    sync_run_id UUID NOT NULL REFERENCES catalog_sync_runs(id) ON DELETE CASCADE,
    external_id VARCHAR(160) NOT NULL,
    price NUMERIC(12,2), unit_price NUMERIC(12,2), available BOOLEAN,
    valid BOOLEAN NOT NULL, raw_data_hash VARCHAR(64) NOT NULL, raw_data JSONB NOT NULL,
    observed_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_staging_price UNIQUE(sync_run_id, external_id)
);

CREATE INDEX idx_staging_categories_run ON staging_categories(sync_run_id);
CREATE INDEX idx_staging_products_run_valid ON staging_products(sync_run_id, valid);
CREATE INDEX idx_staging_prices_run_valid ON staging_prices(sync_run_id, valid);

ALTER TABLE product_price_history ADD COLUMN sync_run_id UUID REFERENCES catalog_sync_runs(id) ON DELETE SET NULL;
ALTER TABLE product_price_history ADD COLUMN source VARCHAR(50);
UPDATE product_price_history SET source = 'LOCAL_JSON' WHERE source IS NULL;
CREATE UNIQUE INDEX uq_product_price_history_sync_run
    ON product_price_history(product_id, sync_run_id) WHERE sync_run_id IS NOT NULL;
CREATE INDEX idx_products_sync_observation ON products(supermarket_id, last_seen_at, available);
