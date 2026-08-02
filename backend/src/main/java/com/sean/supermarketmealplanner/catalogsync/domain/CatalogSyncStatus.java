package com.sean.supermarketmealplanner.catalogsync.domain;

public enum CatalogSyncStatus {
    PENDING, RUNNING, SUCCESS, PARTIAL_SUCCESS, FAILED;
    public boolean terminal() { return this == SUCCESS || this == PARTIAL_SUCCESS || this == FAILED; }
}
