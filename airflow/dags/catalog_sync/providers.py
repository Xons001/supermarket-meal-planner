from __future__ import annotations

import json
import os
from abc import ABC, abstractmethod
from pathlib import Path
from typing import Any


class CatalogProvider(ABC):
    @abstractmethod
    def fetch_catalog(self, supermarket_code: str) -> dict[str, Any]: ...

    def fetch_prices(self, supermarket_code: str) -> list[dict[str, Any]]:
        document = self.fetch_catalog(supermarket_code)
        return [
            {
                "externalId": product.get("externalId"),
                "price": product.get("currentPrice"),
                "unitPrice": product.get("unitPrice"),
                "available": product.get("available"),
            }
            for product in document.get("products", [])
        ]


class LocalJsonCatalogProvider(CatalogProvider):
    def __init__(self, path: str | None = None):
        self.path = Path(path or os.environ.get(
            "CATALOG_PROVIDER_FILE", "/opt/airflow/data/mock/mercadona-catalog.json"
        ))

    def fetch_catalog(self, supermarket_code: str) -> dict[str, Any]:
        document = json.loads(self.path.read_text(encoding="utf-8"))
        if document.get("supermarketCode") != supermarket_code:
            raise ValueError(f"Fixture does not support supermarket {supermarket_code}")
        return document


class ExperimentalMercadonaProvider(CatalogProvider):
    def fetch_catalog(self, supermarket_code: str) -> dict[str, Any]:
        if os.environ.get("MERCADONA_EXPERIMENTAL_PROVIDER_ENABLED", "false").lower() != "true":
            raise RuntimeError("Experimental Mercadona provider is disabled")
        raise NotImplementedError("Real external extraction is outside FASE 9")


def provider(name: str) -> CatalogProvider:
    normalized = name.upper()
    if normalized in {"LOCAL_JSON", "MOCK"}:
        return LocalJsonCatalogProvider()
    if normalized == "MERCADONA_EXPERIMENTAL":
        return ExperimentalMercadonaProvider()
    raise ValueError(f"Unknown catalog provider: {name}")
