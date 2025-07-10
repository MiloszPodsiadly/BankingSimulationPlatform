package com.milosz.podsiadly.infrastructure.integration.worldbank.dto;

import java.util.List;
import java.util.Optional;

public record WorldBankResponse(
        Optional<WorldBankMetadata> metadata,
        List<WorldBankIndicatorData> indicatorData
) {}