package com.sean.supermarketmealplanner.identity.application;

import com.sean.supermarketmealplanner.identity.domain.UserRole;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserProvider {
    public AuthPrincipal require() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthPrincipal principal)) {
            throw new IdentityException(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED",
                    "Debes iniciar sesión para acceder a este recurso");
        }
        return principal;
    }
    public UUID userId() { return require().userId(); }
    public UserRole role() { return require().role(); }
}
