from __future__ import annotations

import hashlib
import json
import os
import re
import unicodedata
import uuid
from datetime import datetime, timedelta, timezone
from difflib import SequenceMatcher
from pathlib import Path
from typing import Any

from psycopg2.extras import Json

from catalog_sync.runtime import connection


def now() -> datetime:
    return datetime.now(timezone.utc)


def canonical_hash(value: Any) -> str:
    raw = json.dumps(value, sort_keys=True, separators=(",", ":"), ensure_ascii=False)
    return hashlib.sha256(raw.encode("utf-8")).hexdigest()


def normalize_name(value: str | None) -> str:
    text = unicodedata.normalize("NFKD", value or "").encode("ascii", "ignore").decode()
    text = re.sub(r"\b(?:pack|paquete|bote|bolsa|lata|botella|formato)\b", " ", text.lower())
    text = re.sub(r"\b\d+(?:[.,]\d+)?\s*(?:kg|g|ml|l|ud|uds)\b", " ", text)
    return " ".join(re.sub(r"[^a-z0-9]+", " ", text).split())


def _nutrition_payload(item: dict[str, Any]) -> dict[str, Any]:
    source = item.get("nutrition") or item
    return {
        "basis": source.get("nutritionBasis", "PER_100_GRAMS"),
        "calories": source.get("caloriesPer100g"),
        "protein": source.get("proteinPer100g"),
        "carbohydrates": source.get("carbohydratesPer100g"),
        "fat": source.get("fatPer100g"),
        "fiber": source.get("fiberPer100g"),
        "sugars": source.get("sugarPer100g"),
        "salt": source.get("saltPer100g"),
        "saturatedFat": source.get("saturatedFatPer100g"),
    }


def _valid(payload: dict[str, Any]) -> bool:
    present = [value for key, value in payload.items() if key != "basis" and value is not None]
    return bool(present) and all(isinstance(value, (int, float)) and value >= 0 for value in present)


def start_run(conf: dict[str, Any], dag_run_id: str) -> dict[str, str]:
    requested = conf.get("runId")
    provider = str(conf.get("provider") or os.getenv("NUTRITION_PROVIDER", "LOCAL_JSON")).upper()
    with connection() as conn, conn.cursor() as cursor:
        if requested:
            run_id = uuid.UUID(requested)
            cursor.execute(
                "UPDATE nutrition_enrichment_runs SET status='RUNNING',started_at=COALESCE(started_at,%s),"
                "airflow_dag_run_id=COALESCE(airflow_dag_run_id,%s),updated_at=%s WHERE id=%s RETURNING id",
                (now(), dag_run_id, now(), str(run_id)),
            )
            if cursor.fetchone() is None:
                raise ValueError(f"Unknown enrichment run {run_id}")
        else:
            run_id = uuid.uuid4()
            cursor.execute(
                "INSERT INTO nutrition_enrichment_runs(id,provider,status,triggered_by,airflow_dag_run_id,"
                "report_json,created_at,updated_at,started_at) VALUES(%s,%s,'RUNNING','SCHEDULED',%s,'{}',%s,%s,%s)",
                (str(run_id), provider, dag_run_id, now(), now(), now()),
            )
    return {"runId": str(run_id), "provider": provider}


def scan_products(ref: dict[str, str]) -> dict[str, str]:
    with connection() as conn, conn.cursor() as cursor:
        cursor.execute("UPDATE nutrition_match_candidates SET status='EXPIRED',row_version=row_version+1 WHERE status='PENDING' AND expires_at<=%s", (now(),))
        cursor.execute(
            "SELECT count(*) FROM products p LEFT JOIN nutrition n ON n.product_id=p.id "
            "WHERE p.available=true AND (n.id IS NULL OR n.verification_status IN ('DEMO','UNVERIFIED') "
            "OR n.completeness<>'COMPLETE' OR n.confidence_score<75)"
        )
        count = cursor.fetchone()[0]
        cursor.execute("UPDATE nutrition_enrichment_runs SET products_scanned=%s,updated_at=%s WHERE id=%s", (count, now(), ref["runId"]))
    return ref


def _fixture() -> list[dict[str, Any]]:
    path = Path(os.getenv("NUTRITION_PROVIDER_FILE", "/opt/airflow/data/mock/mercadona-catalog.json"))
    payload = json.loads(path.read_text(encoding="utf-8"))
    products = list(payload.get("products", payload if isinstance(payload, list) else []))
    overrides = Path(os.getenv("NUTRITION_OVERRIDE_FILE", "/opt/airflow/data/providers/nutrition/local-nutrition-overrides.json"))
    if overrides.exists():
        extra = json.loads(overrides.read_text(encoding="utf-8"))
        products.extend(extra.get("products", extra if isinstance(extra, list) else []))
    return products


def lookup_and_score(ref: dict[str, str]) -> dict[str, str]:
    if ref["provider"] != "LOCAL_JSON":
        raise ValueError("The Airflow enrichment worker only enables the reproducible LOCAL_JSON provider")
    source = _fixture()
    by_barcode = {item.get("barcode"): item for item in source if item.get("barcode")}
    accepted = float(os.getenv("NUTRITION_AUTO_ACCEPT_THRESHOLD", "95"))
    review = float(os.getenv("NUTRITION_MANUAL_REVIEW_THRESHOLD", "75"))
    expires = now() + timedelta(days=int(os.getenv("NUTRITION_REJECTION_COOLDOWN_DAYS", "30")))
    barcode_matches = name_matches = errors = 0
    with connection() as conn, conn.cursor() as cursor:
        cursor.execute(
            "SELECT p.id,p.barcode,p.name,p.brand FROM products p LEFT JOIN nutrition n ON n.product_id=p.id "
            "WHERE p.available=true AND (n.id IS NULL OR n.verification_status IN ('DEMO','UNVERIFIED') "
            "OR n.completeness<>'COMPLETE' OR n.confidence_score<75) ORDER BY p.id"
        )
        for product_id, barcode, name, brand in cursor.fetchall():
            item = by_barcode.get(barcode)
            if item is not None and not _valid(_nutrition_payload(item)):
                item = None
            method = "BARCODE_EXACT" if item else "FUZZY_NAME"
            if item:
                score = 100.0
                barcode_matches += 1
            else:
                normalized = normalize_name(name)
                ranked = sorted(
                    ((SequenceMatcher(None, normalized, normalize_name(candidate.get("name"))).ratio(), candidate) for candidate in source if _valid(_nutrition_payload(candidate))),
                    key=lambda pair: (-pair[0], str(pair[1].get("externalId", ""))),
                )
                ratio, item = ranked[0] if ranked else (0.0, None)
                score = round(ratio * 85 + (10 if item and normalize_name(brand) == normalize_name(item.get("brand")) else 0), 2)
                if score >= review:
                    name_matches += 1
            if not item or score < review:
                continue
            payload = _nutrition_payload(item)
            if not _valid(payload):
                message = f"Invalid nutrition fixture for {item.get('externalId')}"
                error_hash = hashlib.sha256(f"{product_id}:{message}".encode()).hexdigest()
                cursor.execute(
                    "INSERT INTO nutrition_enrichment_errors(id,run_id,product_id,code,message,retryable,error_hash,created_at) "
                    "VALUES(%s,%s,%s,'NUTRITION_DATA_INVALID',%s,false,%s,%s) ON CONFLICT(run_id,error_hash) DO NOTHING",
                    (str(uuid.uuid4()), ref["runId"], product_id, message, error_hash, now()),
                )
                errors += 1
                continue
            reference = str(item.get("externalId") or item.get("barcode") or item.get("name"))
            source_hash = canonical_hash(payload)
            status = "AUTO_ACCEPTED" if score >= accepted else "PENDING"
            cursor.execute(
                "INSERT INTO nutrition_match_candidates(id,run_id,product_id,provider,external_reference,external_barcode,"
                "external_name,normalized_name,brand,candidate_payload_json,match_method,confidence_score,score_breakdown_json,"
                "status,source_hash,expires_at,created_at) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) "
                "ON CONFLICT(product_id,provider,external_reference,source_hash) DO UPDATE SET "
                "run_id=excluded.run_id,expires_at=excluded.expires_at,status=excluded.status "
                "WHERE nutrition_match_candidates.status IN ('PENDING','AUTO_ACCEPTED','EXPIRED')",
                (str(uuid.uuid4()), ref["runId"], product_id, ref["provider"], reference, item.get("barcode"),
                 item.get("name") or name, normalize_name(item.get("name")), item.get("brand"), Json(payload), method,
                 score, Json({"deterministicScore": score, "barcode": method == "BARCODE_EXACT"}), status,
                 source_hash, expires, now()),
            )
        cursor.execute(
            "UPDATE nutrition_enrichment_runs SET barcode_matches=%s,name_matches=%s,errors=errors+%s,updated_at=%s WHERE id=%s",
            (barcode_matches, name_matches, errors, now(), ref["runId"]),
        )
    return ref


def apply_auto_accepted(ref: dict[str, str]) -> dict[str, str]:
    updated = unchanged = 0
    with connection() as conn, conn.cursor() as cursor:
        cursor.execute(
            "SELECT c.product_id,c.candidate_payload_json,c.confidence_score,c.external_reference,c.source_hash,n.id,"
            "n.calories_per_100g,n.protein_per_100g,n.carbohydrates_per_100g,n.fat_per_100g,n.fiber_per_100g,"
            "n.sugar_per_100g,n.salt_per_100g,n.saturated_fat_per_100g,n.data_source,n.verification_status "
            "FROM nutrition_match_candidates c LEFT JOIN nutrition n ON n.product_id=c.product_id "
            "WHERE c.run_id=%s AND c.status='AUTO_ACCEPTED' ORDER BY c.product_id", (ref["runId"],)
        )
        for row in cursor.fetchall():
            product_id, payload, confidence, reference, source_hash, nutrition_id, *old_values = row
            number = lambda value: float(value) if value is not None else None
            before = None if nutrition_id is None else {
                "calories": number(old_values[0]), "protein": number(old_values[1]), "carbohydrates": number(old_values[2]),
                "fat": number(old_values[3]), "fiber": number(old_values[4]), "sugars": number(old_values[5]), "salt": number(old_values[6]),
                "saturatedFat": number(old_values[7]), "dataSource": old_values[8], "verificationStatus": old_values[9],
            }
            if nutrition_id is not None and old_values[9] == "MANUAL_OVERRIDE":
                unchanged += 1
                continue
            values = (payload.get("calories"), payload.get("protein"), payload.get("carbohydrates"), payload.get("fat"),
                      payload.get("fiber"), payload.get("sugars"), payload.get("salt"), payload.get("saturatedFat"))
            completeness = "COMPLETE" if sum(value is not None for value in values) >= 7 else "PARTIAL"
            nutrition_id = nutrition_id or uuid.uuid4()
            cursor.execute(
                "INSERT INTO nutrition(id,product_id,calories_per_100g,protein_per_100g,carbohydrates_per_100g,fat_per_100g,"
                "fiber_per_100g,sugar_per_100g,salt_per_100g,saturated_fat_per_100g,data_source,verification_status,"
                "confidence_score,nutrition_basis,completeness,source_reference,source_updated_at,updated_at,created_at,row_version) "
                "VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,'VERIFIED',%s,%s,%s,%s,%s,%s,%s,0) "
                "ON CONFLICT(product_id) DO UPDATE SET calories_per_100g=excluded.calories_per_100g,protein_per_100g=excluded.protein_per_100g,"
                "carbohydrates_per_100g=excluded.carbohydrates_per_100g,fat_per_100g=excluded.fat_per_100g,fiber_per_100g=excluded.fiber_per_100g,"
                "sugar_per_100g=excluded.sugar_per_100g,salt_per_100g=excluded.salt_per_100g,saturated_fat_per_100g=excluded.saturated_fat_per_100g,"
                "data_source=excluded.data_source,verification_status='VERIFIED',confidence_score=excluded.confidence_score,"
                "nutrition_basis=excluded.nutrition_basis,completeness=excluded.completeness,source_reference=excluded.source_reference,"
                "source_updated_at=excluded.source_updated_at,updated_at=excluded.updated_at,row_version=nutrition.row_version+1 "
                "WHERE nutrition.verification_status<>'MANUAL_OVERRIDE'",
                (str(nutrition_id), product_id, *values, ref["provider"], confidence, payload.get("basis", "PER_100_GRAMS"),
                 completeness, reference, now(), now(), now()),
            )
            after = {**payload, "dataSource": ref["provider"], "verificationStatus": "VERIFIED", "confidenceScore": float(confidence)}
            cursor.execute(
                "INSERT INTO product_nutrition_history(id,product_id,previous_snapshot_json,new_snapshot_json,change_source,provider,"
                "confidence_score,changed_at,reason,snapshot_hash) VALUES(%s,%s,%s,%s,'AUTOMATIC',%s,%s,%s,%s,%s) "
                "ON CONFLICT(product_id,snapshot_hash) DO NOTHING",
                (str(uuid.uuid4()), product_id, Json(before) if before else None, Json(after), ref["provider"], confidence,
                 now(), "Automatic deterministic nutrition enrichment", source_hash),
            )
            updated += 1
        cursor.execute(
            "UPDATE nutrition_enrichment_runs SET auto_accepted=(SELECT count(*) FROM nutrition_match_candidates WHERE run_id=%s AND status='AUTO_ACCEPTED'),"
            "pending_review=(SELECT count(*) FROM nutrition_match_candidates WHERE run_id=%s AND status='PENDING'),"
            "updated_products=%s,unchanged_products=%s,updated_at=%s WHERE id=%s",
            (ref["runId"], ref["runId"], updated, unchanged, now(), ref["runId"]),
        )
    return ref


def finish(ref: dict[str, str]) -> dict[str, str]:
    with connection() as conn, conn.cursor() as cursor:
        cursor.execute("SELECT started_at,errors FROM nutrition_enrichment_runs WHERE id=%s", (ref["runId"],))
        started, errors = cursor.fetchone()
        status = "PARTIAL_SUCCESS" if errors else "SUCCESS"
        ended = now()
        duration = int((ended - started).total_seconds() * 1000) if started else 0
        cursor.execute(
            "UPDATE nutrition_enrichment_runs SET status=%s,finished_at=%s,duration_ms=%s,updated_at=%s,"
            "report_json=jsonb_build_object('status',%s,'provider',provider,'productsScanned',products_scanned,"
            "'barcodeMatches',barcode_matches,'nameMatches',name_matches,'autoAccepted',auto_accepted,"
            "'pendingReview',pending_review,'updatedProducts',updated_products,'unchangedProducts',unchanged_products,'errors',errors) WHERE id=%s",
            (status, ended, duration, ended, status, ref["runId"]),
        )
    return {**ref, "status": status}


def fail_run(run_id: str | None, message: str) -> None:
    if not run_id:
        return
    with connection() as conn, conn.cursor() as cursor:
        cursor.execute(
            "UPDATE nutrition_enrichment_runs SET status='FAILED',finished_at=%s,updated_at=%s,errors=errors+1,"
            "report_json=jsonb_build_object('status','FAILED','message',%s) WHERE id=%s AND status IN ('PENDING','RUNNING')",
            (now(), now(), message[:500], run_id),
        )


def failure_callback(context: dict[str, Any]) -> None:
    conf = context.get("dag_run").conf or {} if context.get("dag_run") else {}
    run_id = conf.get("runId")
    if not run_id and context.get("task_instance"):
        ref = context["task_instance"].xcom_pull(task_ids="start_enrichment_run")
        run_id = ref.get("runId") if isinstance(ref, dict) else None
    fail_run(run_id, str(context.get("exception") or "Airflow enrichment task failed"))
