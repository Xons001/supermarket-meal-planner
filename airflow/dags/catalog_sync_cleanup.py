from __future__ import annotations

import os
from datetime import datetime
from airflow.sdk import dag, task
from catalog_sync.runtime import cleanup


@dag(dag_id="catalog_sync_cleanup", schedule="15 4 * * *", start_date=datetime(2026, 1, 1),
     catchup=False, max_active_runs=1, tags=["catalog", "maintenance", "fase-9"])
def catalog_sync_cleanup():
    @task
    def cleanup_staging():
        return cleanup(int(os.environ.get("CATALOG_SYNC_STAGING_RETENTION_DAYS", "7")))
    cleanup_staging()


catalog_sync_cleanup()
