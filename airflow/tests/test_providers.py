import json
from pathlib import Path

import pytest
from catalog_sync.providers import ExperimentalMercadonaProvider, LocalJsonCatalogProvider
from catalog_sync.runtime import digest, normalize_and_validate


def test_local_provider_reads_repeatable_fixture(tmp_path: Path):
    fixture=tmp_path/"catalog.json"
    fixture.write_text(json.dumps({"supermarketCode":"MERCADONA","categories":[],"products":[]}),encoding="utf-8")
    assert LocalJsonCatalogProvider(str(fixture)).fetch_catalog("MERCADONA")["products"] == []


def test_local_provider_rejects_another_supermarket(tmp_path: Path):
    fixture=tmp_path/"catalog.json"; fixture.write_text(json.dumps({"supermarketCode":"MERCADONA"}),encoding="utf-8")
    with pytest.raises(ValueError): LocalJsonCatalogProvider(str(fixture)).fetch_catalog("LIDL")


def test_experimental_provider_is_disabled(monkeypatch):
    monkeypatch.delenv("MERCADONA_EXPERIMENTAL_PROVIDER_ENABLED",raising=False)
    with pytest.raises(RuntimeError): ExperimentalMercadonaProvider().fetch_catalog("MERCADONA")


def test_hash_is_canonical():
    assert digest({"a":1,"b":2}) == digest({"b":2,"a":1})


def test_invalid_item_is_reported_without_aborting_batch(tmp_path: Path):
    payload = tmp_path / "invalid.json"
    payload.write_text(json.dumps({"categories": [], "products": [
        {"externalId": "invalid-product", "name": "Incomplete"}
    ]}), encoding="utf-8")
    result = normalize_and_validate({"syncRunId": "run", "payloadPath": str(payload)})
    normalized = json.loads(Path(result["normalizedPath"]).read_text(encoding="utf-8"))
    assert result["errorCount"] == 1
    assert normalized["items"][0]["valid"] is False
