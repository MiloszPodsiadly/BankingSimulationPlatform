package com.milosz.podsiadly.infrastructure.integration.worldbank;

import com.milosz.podsiadly.infrastructure.integration.worldbank.dto.WorldBankResponse;
import com.milosz.podsiadly.common.exception.WorldBankApiException;

// Interfejs klienta World Bank API
public interface WorldBankClient { // Zmieniam nazwę na WorldBankClient, aby była spójna z implementacją

    // Metoda zwracająca nowo zdefiniowany rekord WorldBankResponse
    WorldBankResponse getIndicatorData( // Zgodnie z WorldBankClientImpl, metoda nazywa się getIndicatorData
                                        String countryCode,
                                        String indicatorId,
                                        String format, // Format, np. "json"
                                        String dateRange // Zakres dat, np. "2020:2023"
    ) throws WorldBankApiException; // Deklarujemy, że może rzucić wyjątek
}