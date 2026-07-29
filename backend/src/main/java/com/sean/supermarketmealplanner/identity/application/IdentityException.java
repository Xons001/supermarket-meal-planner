package com.sean.supermarketmealplanner.identity.application;

import org.springframework.http.HttpStatus;

public class IdentityException extends RuntimeException {
    private final String code;
    private final HttpStatus status;
    public IdentityException(HttpStatus status, String code, String message) {
        super(message); this.status = status; this.code = code;
    }
    public String getCode() { return code; }
    public HttpStatus getStatus() { return status; }
}
