from __future__ import annotations

import os
from datetime import datetime

from airflow.sdk import dag, task

from nutrition_pipeline.runtime import apply_auto_accepted, failure_callback, finish, lookup_and_score, scan_products, start_run


@dag(
    dag_id="nutrition_enrichment",
    schedule=os.getenv("NUTRITION_ENRICHMENT_CRON", "0 4 * * 1"),
    start_date=datetime(2026, 1, 1),
    catchup=False,
    max_active_runs=1,
    tags=["nutrition", "fase-10"],
    on_failure_callback=failure_callback,
)
def nutrition_enrichment():
    @task
    def start_enrichment_run(**context):
        return start_run(context["dag_run"].conf or {}, context["dag_run"].run_id)

    @task
    def find_products_without_nutrition(ref):
        return scan_products(ref)

    @task
    def find_products_with_low_quality_nutrition(ref):
        return ref

    @task
    def batch_products(ref):
        return ref

    @task
    def lookup_by_barcode(ref):
        return lookup_and_score(ref)

    @task
    def lookup_by_name(ref):
        return ref

    @task
    def score_candidates(ref):
        return ref

    @task
    def auto_accept_high_confidence(ref):
        return apply_auto_accepted(ref)

    @task
    def store_review_candidates(ref):
        return ref

    @task
    def update_nutrition(ref):
        return ref

    @task
    def write_history(ref):
        return ref

    @task
    def finish_enrichment_run(ref):
        return finish(ref)

    @task
    def generate_enrichment_report(ref):
        return {"runId": ref["runId"], "status": ref["status"]}

    ref = start_enrichment_run()
    ref = find_products_without_nutrition(ref)
    ref = find_products_with_low_quality_nutrition(ref)
    ref = batch_products(ref)
    ref = lookup_by_barcode(ref)
    ref = lookup_by_name(ref)
    ref = score_candidates(ref)
    ref = auto_accept_high_confidence(ref)
    ref = store_review_candidates(ref)
    ref = update_nutrition(ref)
    ref = write_history(ref)
    ref = finish_enrichment_run(ref)
    generate_enrichment_report(ref)


nutrition_enrichment()
