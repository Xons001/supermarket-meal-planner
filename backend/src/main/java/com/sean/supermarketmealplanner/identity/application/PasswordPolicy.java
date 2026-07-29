package com.sean.supermarketmealplanner.identity.application;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class PasswordPolicy {
    public void validate(String password) {
        int points = password == null ? 0 : password.codePointCount(0, password.length());
        if (points < 10 || points > 128) {
            throw new IdentityException(HttpStatus.UNPROCESSABLE_ENTITY, "PASSWORD_POLICY_VIOLATION",
                    "La contraseña debe tener entre 10 y 128 caracteres Unicode");
        }
    }
}
