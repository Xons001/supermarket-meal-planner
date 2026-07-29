package com.sean.supermarketmealplanner.identity.application;

import com.sean.supermarketmealplanner.identity.domain.UserRole;
import java.util.UUID;

public record AuthPrincipal(UUID userId, UUID sessionId, UserRole role) {}
