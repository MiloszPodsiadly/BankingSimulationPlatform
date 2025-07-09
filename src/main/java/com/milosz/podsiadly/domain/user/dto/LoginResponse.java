package com.milosz.podsiadly.domain.user.dto;

import java.util.Set;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        Long userId,
        String username,
        Set<String> roles
) {}