package com.milosz.podsiadly.domain.simulation.data.api;

import com.milosz.podsiadly.domain.simulation.data.dto.WorldBankData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
public class EconomicDataApiClient {

    private final RestTemplate restTemplate;

    @Value("${api.worldbank.base-url}")
    private String baseUrl;

    public EconomicDataApiClient(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Fetches economic data for a specific country and indicator.
     * Example indicators:
     * - "FP.CPI.TOTL.ZG" (Inflation, consumer prices (annual %))
     * - "NY.GDP.MKTP.CD" (GDP (current US$))
     * - "SL.UEM.TOTL.ZS" (Unemployment, total (% of total labor force))
     * @param countryCode ISO 2-letter country code (e.g., "PL" for Poland, "US" for United States)
     * @param indicatorCode World Bank indicator code
     * @param format "json" or "xml"
     * @param dateRange Example: "2020:2023" for years 2020 to 2023
     * @return Optional containing WorldBankData response.
     */
    public Optional<WorldBankData.IndicatorData> getEconomicIndicatorData(
            String countryCode, String indicatorCode, String format, String dateRange) {

        String url = String.format("%s%s/indicator/%s?format=%s&date=%s",
                baseUrl, countryCode, indicatorCode, format, dateRange);
        log.info("Fetching economic data from: {}", url);

        try {
            // World Bank API returns a List of JSON arrays, e.g., [[PageInfo], [IndicatorData...]]
            // We need to retrieve it as an array of Objects and then cast/process.
            // For simplicity, we directly try to map to WorldBankData, which implies a custom deserializer
            // or a more robust approach in DTO. A common pattern is to fetch as Object[] and then map.
            // Let's assume the first element of the response array is PageInfo and the second is the list of data.

            // The World Bank API returns a JSON array like:
            // [
            //   {"page":1, "pages":1, "per_page":50, "total":1},
            //   [
            //     {"indicator":{"id":"FP.CPI.TOTL.ZG", "value":"Inflation, consumer prices (annual %)"}, "country":{"id":"PL", "value":"Poland"}, "countryiso3code":"POL", "date":"2023", "value":11.4, "unit":"", "obs_status":"", "decimal":1}
            //   ]
            // ]
            // We are interested in the second element of the top-level array.

            Object[] response = restTemplate.getForObject(url, Object[].class);

            if (response != null && response.length >= 2) {
                // The second element is expected to be a List of Maps (raw data), or directly mappable to List<IndicatorData>
                // We'll rely on Jackson to map it if the DTO structure matches closely.
                // A safer way is to fetch the second element as List<Map<String, Object>> and then map manually.
                // For direct mapping, WorldBankData DTO needs to accommodate this array structure.
                // Re-mapping from raw Object[] might be necessary depending on exact API response.

                @SuppressWarnings("unchecked")
                List<WorldBankData.IndicatorData> indicatorDataList = (List<WorldBankData.IndicatorData>) response[1];

                if (indicatorDataList != null && !indicatorDataList.isEmpty()) {
                    log.info("Successfully fetched {} economic data points for {}-{}", indicatorDataList.size(), countryCode, indicatorCode);
                    // Return the first/most recent data point, or a list, depending on needs.
                    return Optional.of(indicatorDataList.get(0)); // Returning the first data point for simplicity
                } else {
                    log.warn("No economic data found for {}-{}", countryCode, indicatorCode);
                    return Optional.empty();
                }
            } else {
                log.warn("World Bank API response was null or malformed for {}-{}", countryCode, indicatorCode);
                return Optional.empty();
            }
        } catch (HttpClientErrorException e) {
            log.error("HTTP error fetching economic data: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return Optional.empty();
        } catch (Exception e) {
            log.error("Unexpected error fetching economic data: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }
}