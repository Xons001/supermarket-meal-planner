from airflow.models import DagBag


def test_dags_import_without_errors():
    bag=DagBag(dag_folder="/opt/airflow/dags")
    assert bag.import_errors == {}
    assert {"catalog_full_sync","catalog_price_sync","catalog_sync_cleanup"}.issubset(bag.dags)
    assert "expression='0 3 * * *'" in repr(bag.dags["catalog_full_sync"].timetable)
    assert "expression='0 */6 * * *'" in repr(bag.dags["catalog_price_sync"].timetable)
