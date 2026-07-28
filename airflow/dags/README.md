# DAGs previstos

## `supermarket_catalog_sync`

1. `extract_categories`
2. `extract_products`
3. `normalize_products`
4. `validate_products`
5. `load_categories`
6. `load_products`
7. `update_price_history`
8. `mark_missing_products_unavailable`
9. `generate_sync_report`

## `nutrition_enrichment`

1. `find_products_without_nutrition`
2. `search_nutrition_source`
3. `match_products`
4. `validate_match_confidence`
5. `store_nutrition_data`
6. `generate_enrichment_report`

Los DAGs serán idempotentes, trabajarán por lotes y no transportarán catálogos
completos mediante XCom.
