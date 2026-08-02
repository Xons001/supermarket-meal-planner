from __future__ import annotations

import os
from datetime import datetime

from airflow.sdk import dag, task
from catalog_sync.runtime import (
    dag_failure_callback,
    extract_catalog,
    finalize,
    merge_prices,
    normalize_and_validate,
    stage,
    start_run,
)


@dag(
    dag_id="catalog_price_sync",
    schedule=os.environ.get("CATALOG_PRICE_SYNC_SCHEDULE", "0 */6 * * *"),
    start_date=datetime(2026, 1, 1),
    catchup=False,
    max_active_runs=1,
    tags=["catalog", "prices", "fase-9"],
    on_failure_callback=dag_failure_callback,
)
def catalog_price_sync():
    @task
    def start_sync_run(**context):
        return start_run(
            context["dag_run"].conf or {},
            "PRICES_ONLY",
            "catalog_price_sync",
            context["dag_run"].run_id,
        )

    @task
    def fetch_prices(ref):
        return extract_catalog(ref, True)

    @task
    def normalize_prices(ref):
        return normalize_and_validate(ref, True)

    @task
    def validate_prices(ref):
        return ref

    @task
    def load_price_staging(ref):
        return stage(ref, True)

    @task
    def merge_price_changes(ref):
        return merge_prices(ref)

    @task
    def publish_sync_report(ref):
        return finalize(ref, False)

    publish_sync_report(
        merge_price_changes(
            load_price_staging(
                validate_prices(normalize_prices(fetch_prices(start_sync_run())))
            )
        )
    )


catalog_price_sync()
