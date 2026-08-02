from airflow.models import DagBag

from nutrition_pipeline.runtime import canonical_hash, normalize_name


def test_name_normalization_removes_accents_package_and_measurement():
    assert (
        normalize_name("  Lácteos: Yogur natural, pack 4 x 125 g ")
        == "lacteos yogur natural 4 x"
    )


def test_source_hash_is_deterministic_for_key_order():
    assert canonical_hash({"protein": 10, "calories": 100}) == canonical_hash(
        {"calories": 100, "protein": 10}
    )


def test_nutrition_dag_has_expected_observable_steps():
    dag = DagBag(dag_folder="/opt/airflow/dags").get_dag("nutrition_enrichment")
    assert dag is not None
    assert dag.max_active_runs == 1
    assert {
        "start_enrichment_run",
        "lookup_by_barcode",
        "lookup_by_name",
        "score_candidates",
        "auto_accept_high_confidence",
        "store_review_candidates",
        "write_history",
        "generate_enrichment_report",
    }.issubset(dag.task_ids)
