from __future__ import annotations

import os
from datetime import datetime

from airflow.sdk import dag, task

from catalog_sync.runtime import dag_failure_callback, extract_catalog, finalize, merge_catalog, normalize_and_validate, stage, start_run


@dag(
    dag_id="catalog_full_sync",
    schedule=os.environ.get("CATALOG_FULL_SYNC_SCHEDULE", "0 3 * * *"),
    start_date=datetime(2026, 1, 1),
    catchup=False,
    max_active_runs=1,
    tags=["catalog", "fase-9"],
    on_failure_callback=dag_failure_callback,
)
def catalog_full_sync():
    @task
    def start_sync_run(**context):
        return start_run(context["dag_run"].conf or {}, "FULL_CATALOG", "catalog_full_sync", context["dag_run"].run_id)

    @task
    def fetch_categories(ref):
        return extract_catalog(ref)

    @task
    def fetch_products(ref):
        return ref

    @task
    def normalize_data(ref):
        return normalize_and_validate(ref)

    @task
    def validate_data(ref):
        return ref

    @task
    def load_staging(ref):
        return stage(ref)

    @task
    def merge_categories(ref):
        return ref

    @task
    def merge_products(ref):
        return merge_catalog(ref)

    @task
    def update_price_history(ref):
        return ref

    @task
    def mark_unavailable(ref):
        return finalize(ref, True)

    @task
    def publish_sync_report(ref):
        return {"syncRunId": ref["syncRunId"], "status": ref["status"]}

    publish_sync_report(mark_unavailable(update_price_history(merge_products(merge_categories(load_staging(
        validate_data(normalize_data(fetch_products(fetch_categories(start_sync_run()))))))))))


catalog_full_sync()
