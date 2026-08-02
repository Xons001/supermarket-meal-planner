from __future__ import annotations

import hashlib
import json
import os
import uuid
from contextlib import contextmanager
from datetime import datetime, timedelta, timezone
from pathlib import Path
from typing import Any, Iterator

import psycopg2
from psycopg2.extras import Json, execute_values

from catalog_sync.providers import provider


@contextmanager
def connection() -> Iterator[Any]:
    conn = psycopg2.connect(
        host=os.environ.get("APP_DB_HOST", "postgres"),
        port=int(os.environ.get("APP_DB_PORT", "5432")),
        dbname=os.environ.get("APP_DB_NAME", "meal_planner"),
        user=os.environ.get("APP_DB_USER", "meal_planner"),
        password=os.environ["APP_DB_PASSWORD"],
    )
    try:
        yield conn
        conn.commit()
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()


def utcnow() -> datetime:
    return datetime.now(timezone.utc)


def digest(value: dict[str, Any]) -> str:
    canonical = json.dumps(
        value, sort_keys=True, separators=(",", ":"), ensure_ascii=False
    )
    return hashlib.sha256(canonical.encode("utf-8")).hexdigest()


def start_run(
    conf: dict[str, Any], sync_type: str, dag_id: str, dag_run_id: str
) -> dict[str, str]:
    requested = conf.get("syncRunId")
    supermarket = conf.get("supermarketCode", "MERCADONA")
    provider_name = conf.get(
        "provider", os.environ.get("CATALOG_SYNC_PROVIDER", "LOCAL_JSON")
    )
    request_id = str(conf.get("requestId") or "")[:64]
    with connection() as conn, conn.cursor() as cursor:
        if requested:
            run_id = uuid.UUID(requested)
            cursor.execute(
                "UPDATE catalog_sync_runs SET status='RUNNING', started_at=COALESCE(started_at,%s), "
                "airflow_dag_id=%s, airflow_dag_run_id=COALESCE(airflow_dag_run_id,%s), updated_at=%s "
                "WHERE id=%s RETURNING id",
                (utcnow(), dag_id, dag_run_id, utcnow(), str(run_id)),
            )
            if cursor.fetchone() is None:
                raise ValueError(f"Unknown sync run {run_id}")
        else:
            run_id = uuid.uuid4()
            cursor.execute(
                "SELECT id FROM supermarkets WHERE code=%s AND enabled=true",
                (supermarket,),
            )
            row = cursor.fetchone()
            if not row:
                raise ValueError(f"Unsupported supermarket {supermarket}")
            cursor.execute(
                "INSERT INTO catalog_sync_runs(id,supermarket_id,sync_type,triggered_by,status,provider,airflow_dag_id,"
                "airflow_dag_run_id,configuration_json,requested_at,started_at,updated_at) "
                "VALUES(%s,%s,%s,'SCHEDULED','RUNNING',%s,%s,%s,%s,%s,%s,%s)",
                (
                    str(run_id),
                    row[0],
                    sync_type,
                    provider_name,
                    dag_id,
                    dag_run_id,
                    Json({"provider": provider_name, "requestId": request_id or None}),
                    utcnow(),
                    utcnow(),
                    utcnow(),
                ),
            )
    return {
        "syncRunId": str(run_id),
        "supermarketCode": supermarket,
        "provider": provider_name,
        "requestId": request_id,
    }


def extract_catalog(ref: dict[str, str], prices_only: bool = False) -> dict[str, str]:
    source = provider(ref["provider"])
    payload: Any = (
        source.fetch_prices(ref["supermarketCode"])
        if prices_only
        else source.fetch_catalog(ref["supermarketCode"])
    )
    directory = Path(os.environ.get("CATALOG_SYNC_TMP_DIR", "/opt/airflow/catalog-tmp"))
    directory.mkdir(parents=True, exist_ok=True)
    path = directory / f"{ref['syncRunId']}.json"
    path.write_text(json.dumps(payload, ensure_ascii=False), encoding="utf-8")
    return {**ref, "payloadPath": str(path)}


def normalize_and_validate(
    ref: dict[str, str], prices_only: bool = False
) -> dict[str, Any]:
    payload = json.loads(Path(ref["payloadPath"]).read_text(encoding="utf-8"))
    categories = [] if prices_only else payload.get("categories", [])
    products = payload if prices_only else payload.get("products", [])
    normalized: list[dict[str, Any]] = []
    errors: list[dict[str, Any]] = []
    for item in products:
        required = (
            ["externalId", "price", "unitPrice"]
            if prices_only
            else [
                "externalId",
                "categoryExternalId",
                "name",
                "currentPrice",
                "unitPrice",
                "packageQuantity",
                "packageUnit",
            ]
        )
        missing = [key for key in required if item.get(key) is None]
        valid = not missing
        row = dict(item)
        row["valid"] = valid
        row["rawDataHash"] = digest(item)
        normalized.append(row)
        if missing:
            errors.append(
                {
                    "entityType": "PRICE" if prices_only else "PRODUCT",
                    "externalId": item.get("externalId"),
                    "errorCode": "REQUIRED_FIELD_MISSING",
                    "message": "Missing: " + ", ".join(missing),
                    "rawDataHash": row["rawDataHash"],
                }
            )
    output = {"categories": categories, "items": normalized, "errors": errors}
    target = Path(ref["payloadPath"]).with_suffix(".normalized.json")
    target.write_text(json.dumps(output, ensure_ascii=False), encoding="utf-8")
    return {
        **ref,
        "normalizedPath": str(target),
        "itemCount": len(normalized),
        "errorCount": len(errors),
    }


def stage(ref: dict[str, Any], prices_only: bool = False) -> dict[str, Any]:
    payload = json.loads(Path(ref["normalizedPath"]).read_text(encoding="utf-8"))
    run_id = ref["syncRunId"]
    observed = utcnow()
    with connection() as conn, conn.cursor() as cursor:
        if not prices_only:
            categories = [
                (
                    run_id,
                    c.get("externalId"),
                    c.get("name"),
                    c.get("parentExternalId"),
                    bool(c.get("externalId") and c.get("name")),
                    digest(c),
                    Json(c),
                    observed,
                )
                for c in payload["categories"]
            ]
            if categories:
                execute_values(
                    cursor,
                    "INSERT INTO staging_categories(sync_run_id,external_id,name,parent_external_id,valid,raw_data_hash,raw_data,observed_at) VALUES %s ON CONFLICT(sync_run_id,external_id) DO UPDATE SET name=excluded.name,parent_external_id=excluded.parent_external_id,valid=excluded.valid,raw_data_hash=excluded.raw_data_hash,raw_data=excluded.raw_data,observed_at=excluded.observed_at",
                    categories,
                )
            rows = []
            for p in payload["items"]:
                unit = p.get("packageUnit")
                measurement = (
                    "WEIGHT"
                    if unit in {"G", "KG"}
                    else "VOLUME"
                    if unit in {"ML", "L"}
                    else "UNIT"
                )
                rows.append(
                    (
                        run_id,
                        p.get("externalId"),
                        p.get("categoryExternalId"),
                        p.get("barcode"),
                        p.get("name"),
                        p.get("brand"),
                        p.get("description"),
                        p.get("imageUrl"),
                        p.get("productUrl"),
                        p.get("currentPrice"),
                        p.get("unitPrice"),
                        p.get("packageQuantity"),
                        unit,
                        measurement,
                        p.get("costDataComplete", True),
                        p.get("available", True),
                        p.get("source", "LOCAL_JSON"),
                        p["valid"],
                        p["rawDataHash"],
                        Json(p),
                        observed,
                    )
                )
            if rows:
                execute_values(
                    cursor,
                    "INSERT INTO staging_products(sync_run_id,external_id,category_external_id,barcode,name,brand,description,image_url,product_url,current_price,unit_price,package_quantity,package_unit,measurement_type,cost_data_complete,available,source,valid,raw_data_hash,raw_data,observed_at) VALUES %s ON CONFLICT(sync_run_id,external_id) DO UPDATE SET category_external_id=excluded.category_external_id,barcode=excluded.barcode,name=excluded.name,brand=excluded.brand,description=excluded.description,image_url=excluded.image_url,product_url=excluded.product_url,current_price=excluded.current_price,unit_price=excluded.unit_price,package_quantity=excluded.package_quantity,package_unit=excluded.package_unit,measurement_type=excluded.measurement_type,cost_data_complete=excluded.cost_data_complete,available=excluded.available,source=excluded.source,valid=excluded.valid,raw_data_hash=excluded.raw_data_hash,raw_data=excluded.raw_data,observed_at=excluded.observed_at",
                    rows,
                )
        else:
            rows = [
                (
                    run_id,
                    p.get("externalId"),
                    p.get("price"),
                    p.get("unitPrice"),
                    p.get("available"),
                    p["valid"],
                    p["rawDataHash"],
                    Json(p),
                    observed,
                )
                for p in payload["items"]
            ]
            if rows:
                execute_values(
                    cursor,
                    "INSERT INTO staging_prices(sync_run_id,external_id,price,unit_price,available,valid,raw_data_hash,raw_data,observed_at) VALUES %s ON CONFLICT(sync_run_id,external_id) DO UPDATE SET price=excluded.price,unit_price=excluded.unit_price,available=excluded.available,valid=excluded.valid,raw_data_hash=excluded.raw_data_hash,raw_data=excluded.raw_data,observed_at=excluded.observed_at",
                    rows,
                )
        error_rows = [
            (
                str(uuid.uuid4()),
                run_id,
                "ERROR",
                e["entityType"],
                e.get("externalId"),
                e["errorCode"],
                e["message"],
                e["rawDataHash"],
                observed,
            )
            for e in payload["errors"]
        ]
        if error_rows:
            execute_values(
                cursor,
                "INSERT INTO catalog_sync_errors(id,sync_run_id,severity,entity_type,external_id,error_code,message,raw_data_hash,created_at) VALUES %s ON CONFLICT(sync_run_id,entity_type,external_id,error_code,raw_data_hash) DO NOTHING",
                error_rows,
            )
    return ref


def merge_catalog(ref: dict[str, Any]) -> dict[str, Any]:
    run_id = ref["syncRunId"]
    with connection() as conn, conn.cursor() as cursor:
        cursor.execute(
            "SELECT supermarket_id FROM catalog_sync_runs WHERE id=%s", (run_id,)
        )
        supermarket_id = cursor.fetchone()[0]
        cursor.execute(
            "SELECT count(*) FROM staging_products s LEFT JOIN products p ON p.supermarket_id=%s AND p.external_id=s.external_id WHERE s.sync_run_id=%s AND s.valid AND p.id IS NULL",
            (supermarket_id, run_id),
        )
        created = cursor.fetchone()[0]
        cursor.execute(
            "SELECT count(*) FROM staging_products s JOIN categories c ON c.supermarket_id=%s AND c.external_id=s.category_external_id JOIN products p ON p.supermarket_id=%s AND p.external_id=s.external_id WHERE s.sync_run_id=%s AND s.valid AND ROW(p.category_id,p.barcode,p.name,p.brand,p.description,p.image_url,p.product_url,p.current_price,p.unit_price,p.package_quantity,p.package_unit,p.measurement_type,p.cost_data_complete,p.available,p.source) IS DISTINCT FROM ROW(c.id,s.barcode,s.name,s.brand,s.description,s.image_url,s.product_url,s.current_price,s.unit_price,s.package_quantity,s.package_unit,s.measurement_type,s.cost_data_complete,COALESCE(s.available,true),s.source)",
            (supermarket_id, supermarket_id, run_id),
        )
        updated = cursor.fetchone()[0]
        cursor.execute(
            "INSERT INTO categories(id,supermarket_id,external_id,name,active) SELECT gen_random_uuid(),%s,external_id,name,true FROM staging_categories WHERE sync_run_id=%s AND valid ON CONFLICT(supermarket_id,external_id) DO UPDATE SET name=excluded.name,active=true",
            (supermarket_id, run_id),
        )
        cursor.execute(
            "SELECT p.id,p.external_id,p.current_price,p.unit_price FROM products p JOIN staging_products s ON s.external_id=p.external_id AND s.sync_run_id=%s AND s.valid WHERE p.supermarket_id=%s",
            (run_id, supermarket_id),
        )
        existing = {row[1]: row for row in cursor.fetchall()}
        cursor.execute(
            "INSERT INTO products(id,supermarket_id,category_id,external_id,barcode,name,brand,description,image_url,product_url,current_price,unit_price,package_quantity,package_unit,measurement_type,cost_data_complete,available,source,last_synced_at,last_seen_at,unavailable_since,created_at,updated_at) SELECT gen_random_uuid(),%s,c.id,s.external_id,s.barcode,s.name,s.brand,s.description,s.image_url,s.product_url,s.current_price,s.unit_price,s.package_quantity,s.package_unit,s.measurement_type,s.cost_data_complete,COALESCE(s.available,true),s.source,s.observed_at,s.observed_at,CASE WHEN COALESCE(s.available,true) THEN NULL ELSE s.observed_at END,s.observed_at,s.observed_at FROM staging_products s JOIN categories c ON c.supermarket_id=%s AND c.external_id=s.category_external_id WHERE s.sync_run_id=%s AND s.valid ON CONFLICT(supermarket_id,external_id) DO UPDATE SET category_id=excluded.category_id,barcode=excluded.barcode,name=excluded.name,brand=excluded.brand,description=excluded.description,image_url=excluded.image_url,product_url=excluded.product_url,current_price=excluded.current_price,unit_price=excluded.unit_price,package_quantity=excluded.package_quantity,package_unit=excluded.package_unit,measurement_type=excluded.measurement_type,cost_data_complete=excluded.cost_data_complete,available=excluded.available,source=excluded.source,last_synced_at=excluded.last_synced_at,last_seen_at=excluded.last_seen_at,unavailable_since=CASE WHEN excluded.available THEN NULL ELSE COALESCE(products.unavailable_since,excluded.unavailable_since) END,updated_at=CASE WHEN ROW(products.category_id,products.barcode,products.name,products.brand,products.description,products.image_url,products.product_url,products.current_price,products.unit_price,products.package_quantity,products.package_unit,products.measurement_type,products.cost_data_complete,products.available,products.source) IS DISTINCT FROM ROW(excluded.category_id,excluded.barcode,excluded.name,excluded.brand,excluded.description,excluded.image_url,excluded.product_url,excluded.current_price,excluded.unit_price,excluded.package_quantity,excluded.package_unit,excluded.measurement_type,excluded.cost_data_complete,excluded.available,excluded.source) THEN excluded.updated_at ELSE products.updated_at END",
            (supermarket_id, supermarket_id, run_id),
        )
        cursor.execute(
            "SELECT p.id,s.external_id,s.current_price,s.unit_price,s.observed_at,s.source FROM staging_products s JOIN products p ON p.supermarket_id=%s AND p.external_id=s.external_id WHERE s.sync_run_id=%s AND s.valid",
            (supermarket_id, run_id),
        )
        changes = []
        for (
            product_id,
            external_id,
            price,
            unit_price,
            observed,
            source,
        ) in cursor.fetchall():
            old = existing.get(external_id)
            if old is None or old[2] != price or old[3] != unit_price:
                changes.append(
                    (
                        str(uuid.uuid4()),
                        product_id,
                        price,
                        unit_price,
                        observed,
                        run_id,
                        source,
                    )
                )
        if changes:
            execute_values(
                cursor,
                "INSERT INTO product_price_history(id,product_id,price,unit_price,recorded_at,sync_run_id,source) VALUES %s ON CONFLICT DO NOTHING",
                changes,
            )
        cursor.execute(
            "UPDATE catalog_sync_runs SET categories_processed=(SELECT count(*) FROM staging_categories WHERE sync_run_id=%s AND valid),products_processed=(SELECT count(*) FROM staging_products WHERE sync_run_id=%s AND valid),products_created=%s,products_updated=%s,prices_changed=%s,validation_errors=(SELECT count(*) FROM catalog_sync_errors WHERE sync_run_id=%s),updated_at=%s WHERE id=%s",
            (run_id, run_id, created, updated, len(changes), run_id, utcnow(), run_id),
        )
    return ref


def merge_prices(ref: dict[str, Any]) -> dict[str, Any]:
    run_id = ref["syncRunId"]
    with connection() as conn, conn.cursor() as cursor:
        cursor.execute(
            "SELECT supermarket_id FROM catalog_sync_runs WHERE id=%s", (run_id,)
        )
        supermarket_id = cursor.fetchone()[0]
        cursor.execute(
            "SELECT p.id,s.price,s.unit_price,s.available,s.observed_at FROM staging_prices s JOIN products p ON p.supermarket_id=%s AND p.external_id=s.external_id WHERE s.sync_run_id=%s AND s.valid AND (p.current_price IS DISTINCT FROM s.price OR p.unit_price IS DISTINCT FROM s.unit_price)",
            (supermarket_id, run_id),
        )
        changes = cursor.fetchall()
        if changes:
            execute_values(
                cursor,
                "INSERT INTO product_price_history(id,product_id,price,unit_price,recorded_at,sync_run_id,source) VALUES %s ON CONFLICT DO NOTHING",
                [
                    (str(uuid.uuid4()), r[0], r[1], r[2], r[4], run_id, ref["provider"])
                    for r in changes
                ],
            )
        cursor.execute(
            "UPDATE products p SET current_price=s.price,unit_price=s.unit_price,last_synced_at=s.observed_at,last_seen_at=s.observed_at,available=COALESCE(s.available,p.available),unavailable_since=CASE WHEN s.available=true THEN NULL WHEN s.available=false THEN COALESCE(p.unavailable_since,s.observed_at) ELSE p.unavailable_since END,updated_at=CASE WHEN p.current_price IS DISTINCT FROM s.price OR p.unit_price IS DISTINCT FROM s.unit_price OR (s.available IS NOT NULL AND p.available IS DISTINCT FROM s.available) THEN s.observed_at ELSE p.updated_at END FROM staging_prices s WHERE s.sync_run_id=%s AND s.valid AND p.supermarket_id=%s AND p.external_id=s.external_id",
            (run_id, supermarket_id),
        )
        cursor.execute(
            "UPDATE catalog_sync_runs SET products_processed=(SELECT count(*) FROM staging_prices WHERE sync_run_id=%s AND valid),prices_changed=%s,validation_errors=(SELECT count(*) FROM catalog_sync_errors WHERE sync_run_id=%s),updated_at=%s WHERE id=%s",
            (run_id, len(changes), run_id, utcnow(), run_id),
        )
    return ref


def finalize(ref: dict[str, Any], full_catalog: bool) -> dict[str, Any]:
    run_id = ref["syncRunId"]
    with connection() as conn, conn.cursor() as cursor:
        cursor.execute(
            "SELECT supermarket_id,validation_errors FROM catalog_sync_runs WHERE id=%s",
            (run_id,),
        )
        supermarket_id, error_count = cursor.fetchone()
        unavailable = 0
        if full_catalog and error_count == 0:
            cursor.execute(
                "UPDATE products p SET available=false,unavailable_since=COALESCE(unavailable_since,%s),last_synced_at=%s,updated_at=%s WHERE p.supermarket_id=%s AND p.available=true AND NOT EXISTS(SELECT 1 FROM staging_products s WHERE s.sync_run_id=%s AND s.valid AND s.external_id=p.external_id)",
                (utcnow(), utcnow(), utcnow(), supermarket_id, run_id),
            )
            unavailable = cursor.rowcount
        status = "PARTIAL_SUCCESS" if error_count else "SUCCESS"
        cursor.execute(
            "UPDATE catalog_sync_runs SET status=%s,products_unavailable=%s,completed_at=%s,updated_at=%s,result_json=jsonb_build_object('status',%s,'requestId',%s,'categoriesProcessed',categories_processed,'productsProcessed',products_processed,'productsCreated',products_created,'productsUpdated',products_updated,'productsUnavailable',%s,'pricesChanged',prices_changed,'validationErrors',validation_errors) WHERE id=%s",
            (
                status,
                unavailable,
                utcnow(),
                utcnow(),
                status,
                ref.get("requestId") or None,
                unavailable,
                run_id,
            ),
        )
    return {**ref, "status": status}


def cleanup(retention_days: int) -> dict[str, int]:
    with connection() as conn, conn.cursor() as cursor:
        cursor.execute(
            "DELETE FROM staging_prices WHERE observed_at < now()-(%s * interval '1 day')",
            (retention_days,),
        )
        prices = cursor.rowcount
        cursor.execute(
            "DELETE FROM staging_products WHERE observed_at < now()-(%s * interval '1 day')",
            (retention_days,),
        )
        products = cursor.rowcount
        cursor.execute(
            "DELETE FROM staging_categories WHERE observed_at < now()-(%s * interval '1 day')",
            (retention_days,),
        )
        categories = cursor.rowcount
        cursor.execute(
            "DELETE FROM refresh_token_sessions WHERE revoked_at IS NOT NULL AND expires_at < now()-interval '30 days'"
        )
        sessions = cursor.rowcount
        cursor.execute(
            "DELETE FROM nutrition_match_candidates WHERE expires_at < now()-interval '30 days'"
        )
        candidates = cursor.rowcount
    removed_files = 0
    cutoff = utcnow() - timedelta(days=retention_days)
    directory = Path(os.environ.get("CATALOG_SYNC_TMP_DIR", "/opt/airflow/catalog-tmp"))
    if directory.exists():
        for path in directory.glob("*.json"):
            modified = datetime.fromtimestamp(path.stat().st_mtime, timezone.utc)
            if modified < cutoff:
                path.unlink()
                removed_files += 1
    removed_logs = 0
    log_cutoff = utcnow() - timedelta(
        days=int(os.environ.get("AIRFLOW_LOG_RETENTION_DAYS", "14"))
    )
    log_directory = Path(os.environ.get("AIRFLOW_HOME", "/opt/airflow")) / "logs"
    if log_directory.exists():
        for path in log_directory.rglob("*.log"):
            if datetime.fromtimestamp(path.stat().st_mtime, timezone.utc) < log_cutoff:
                path.unlink()
                removed_logs += 1
    return {
        "categories": categories,
        "products": products,
        "prices": prices,
        "files": removed_files,
        "logs": removed_logs,
        "sessions": sessions,
        "candidates": candidates,
    }


def fail_run(run_id: str | None, message: str) -> None:
    if not run_id:
        return
    safe_message = message[:500]
    with connection() as conn, conn.cursor() as cursor:
        cursor.execute(
            "UPDATE catalog_sync_runs SET status='FAILED',completed_at=%s,updated_at=%s,"
            "result_json=jsonb_build_object('status','FAILED','message',%s) "
            "WHERE id=%s AND status IN ('PENDING','RUNNING')",
            (utcnow(), utcnow(), safe_message, run_id),
        )


def dag_failure_callback(context: dict[str, Any]) -> None:
    conf = (context.get("dag_run").conf or {}) if context.get("dag_run") else {}
    run_id = conf.get("syncRunId")
    if not run_id and context.get("task_instance"):
        ref = context["task_instance"].xcom_pull(task_ids="start_sync_run")
        if isinstance(ref, dict):
            run_id = ref.get("syncRunId")
    fail_run(run_id, str(context.get("exception") or "Airflow task failed"))
