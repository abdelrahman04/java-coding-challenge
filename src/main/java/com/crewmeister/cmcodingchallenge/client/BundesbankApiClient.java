package com.crewmeister.cmcodingchallenge.client;

import com.crewmeister.cmcodingchallenge.exception.ExternalApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Client for fetching exchange rate data from the Bundesbank API.
 * 
 * The Bundesbank provides daily exchange rates through their SDMX REST API.
 * Data is requested in CSV format for easier parsing.
 */
@Component
public class BundesbankApiClient {

    private static final Logger logger = LoggerFactory.getLogger(BundesbankApiClient.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final RestTemplate restTemplate;
    private final String baseUrl;

    // Currency codes available from Bundesbank with their full names
    private static final Map<String, String> SUPPORTED_CURRENCIES;

    static {
        Map<String, String> currencies = new LinkedHashMap<>();
        currencies.put("USD", "US Dollar");
        currencies.put("JPY", "Japanese Yen");
        currencies.put("GBP", "British Pound Sterling");
        currencies.put("CHF", "Swiss Franc");
        currencies.put("AUD", "Australian Dollar");
        currencies.put("CAD", "Canadian Dollar");
        currencies.put("SEK", "Swedish Krona");
        currencies.put("NOK", "Norwegian Krone");
        currencies.put("DKK", "Danish Krone");
        currencies.put("NZD", "New Zealand Dollar");
        currencies.put("PLN", "Polish Zloty");
        currencies.put("HUF", "Hungarian Forint");
        currencies.put("CZK", "Czech Koruna");
        currencies.put("TRY", "Turkish Lira");
        currencies.put("ZAR", "South African Rand");
        currencies.put("MXN", "Mexican Peso");
        currencies.put("BRL", "Brazilian Real");
        currencies.put("CNY", "Chinese Yuan Renminbi");
        currencies.put("INR", "Indian Rupee");
        currencies.put("KRW", "South Korean Won");
        currencies.put("SGD", "Singapore Dollar");
        currencies.put("HKD", "Hong Kong Dollar");
        currencies.put("THB", "Thai Baht");
        currencies.put("MYR", "Malaysian Ringgit");
        currencies.put("PHP", "Philippine Peso");
        currencies.put("IDR", "Indonesian Rupiah");
        SUPPORTED_CURRENCIES = Collections.unmodifiableMap(currencies);
    }

    public BundesbankApiClient(
            RestTemplate restTemplate,
            @Value("${bundesbank.api.base-url:https://api.statistiken.bundesbank.de/rest/data}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    /**
     * Returns map of supported currency codes to their names.
     */
    public Map<String, String> getSupportedCurrencies() {
        return SUPPORTED_CURRENCIES;
    }

    /**
     * Fetches exchange rates for a specific currency from the Bundesbank API.
     * Returns a map of dates to exchange rates.
     * 
     * @param currencyCode ISO currency code (e.g., USD, GBP)
     * @return Map of LocalDate to BigDecimal exchange rates
     */
    public Map<LocalDate, BigDecimal> fetchExchangeRates(String currencyCode) {
        if (!SUPPORTED_CURRENCIES.containsKey(currencyCode.toUpperCase())) {
            logger.warn("Unsupported currency code requested: {}", currencyCode);
            return Collections.emptyMap();
        }

        String url = buildApiUrl(currencyCode.toUpperCase());
        logger.info("Fetching exchange rates for {} from Bundesbank API", currencyCode);

        try {
            // Set Accept header for CSV format
            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "text/csv");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            String csvResponse = response.getBody();
            
            if (logger.isDebugEnabled() && csvResponse != null) {
                String preview = csvResponse.length() > 500 ? csvResponse.substring(0, 500) : csvResponse;
                logger.debug("API Response preview: {}", preview);
            }
            
            return parseExchangeRateResponse(csvResponse);
        } catch (RestClientException e) {
            logger.error("Failed to fetch exchange rates for {}: {}", currencyCode, e.getMessage());
            // Return empty map instead of throwing, so other currencies can still be fetched
            return Collections.emptyMap();
        }
    }

    /**
     * Builds the API URL for fetching exchange rates.
     * Uses the BBEX3 series which contains daily EUR exchange rates.
     */
    private String buildApiUrl(String currencyCode) {
        // BBEX3 is the series identifier for EUR foreign exchange rates
        // D = daily frequency
        // Format: BBEX3/D.{CURRENCY}.EUR.BB.AC.000
        return String.format("%s/BBEX3/D.%s.EUR.BB.AC.000", baseUrl, currencyCode);
    }

    /**
     * Parses the CSV response from Bundesbank API.
     * The SDMX-CSV format has headers and data with semicolon or comma separators.
     */
    private Map<LocalDate, BigDecimal> parseExchangeRateResponse(String csvResponse) {
        Map<LocalDate, BigDecimal> rates = new TreeMap<>();

        if (csvResponse == null || csvResponse.trim().isEmpty()) {
            logger.warn("Empty response received from Bundesbank API");
            return rates;
        }

        String[] lines = csvResponse.split("\\r?\\n");
        int timePeriodIndex = -1;
        int obsValueIndex = -1;
        String delimiter = ",";

        for (String line : lines) {
            if (line.trim().isEmpty()) {
                continue;
            }

            // Detect delimiter (CSV can use comma or semicolon)
            if (line.contains(";")) {
                delimiter = ";";
            }

            String[] parts = line.split(delimiter);

            // Find header row and identify column indices
            if (timePeriodIndex == -1) {
                for (int i = 0; i < parts.length; i++) {
                    String col = parts[i].trim().replace("\"", "").toUpperCase();
                    if (col.equals("TIME_PERIOD")) {
                        timePeriodIndex = i;
                    } else if (col.equals("OBS_VALUE")) {
                        obsValueIndex = i;
                    }
                }
                // If we found the headers, continue to next line for data
                if (timePeriodIndex != -1 && obsValueIndex != -1) {
                    continue;
                }
                // If this line has headers, skip it
                if (timePeriodIndex != -1 || obsValueIndex != -1) {
                    continue;
                }
                continue;
            }

            // Parse data rows
            if (parts.length > Math.max(timePeriodIndex, obsValueIndex)) {
                try {
                    String dateStr = parts[timePeriodIndex].trim().replace("\"", "");
                    String valueStr = parts[obsValueIndex].trim().replace("\"", "");

                    // Skip if value is empty or marked as not available
                    if (valueStr.isEmpty() || valueStr.equals(".") || valueStr.equals("-") || valueStr.equalsIgnoreCase("NaN")) {
                        continue;
                    }

                    LocalDate date = parseDate(dateStr);
                    BigDecimal rate = new BigDecimal(valueStr);

                    if (date != null && rate.compareTo(BigDecimal.ZERO) > 0) {
                        rates.put(date, rate);
                    }
                } catch (NumberFormatException e) {
                    logger.trace("Skipping invalid rate value in line: {}", line);
                } catch (Exception e) {
                    logger.trace("Error parsing line: {} - {}", line, e.getMessage());
                }
            }
        }

        logger.info("Parsed {} exchange rate entries", rates.size());
        return rates;
    }

    /**
     * Parses date strings in various formats used by Bundesbank.
     */
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        
        try {
            // Handle yyyy-MM-dd format
            if (dateStr.matches("\\d{4}-\\d{2}-\\d{2}")) {
                return LocalDate.parse(dateStr, DATE_FORMATTER);
            }
            // Handle yyyy-MM format (monthly data)
            if (dateStr.matches("\\d{4}-\\d{2}")) {
                return LocalDate.parse(dateStr + "-01", DATE_FORMATTER);
            }
            // Handle yyyy format (yearly data)
            if (dateStr.matches("\\d{4}")) {
                return LocalDate.parse(dateStr + "-01-01", DATE_FORMATTER);
            }
        } catch (Exception e) {
            logger.trace("Failed to parse date: {}", dateStr);
        }
        return null;
    }
}
