package com.crewmeister.cmcodingchallenge.client;

import com.crewmeister.cmcodingchallenge.exception.ExternalApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
 * The Bundesbank provides daily exchange rates through their statistics API.
 * Data format is CSV with time series information.
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
            String response = restTemplate.getForObject(url, String.class);
            return parseExchangeRateResponse(response);
        } catch (RestClientException e) {
            logger.error("Failed to fetch exchange rates for {}: {}", currencyCode, e.getMessage());
            throw new ExternalApiException("Failed to fetch exchange rates from Bundesbank API", e);
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
        return String.format("%s/BBEX3/D.%s.EUR.BB.AC.000?format=csv", baseUrl, currencyCode);
    }

    /**
     * Parses the CSV response from Bundesbank API.
     * The CSV format contains time series data with date and value columns.
     */
    private Map<LocalDate, BigDecimal> parseExchangeRateResponse(String csvResponse) {
        Map<LocalDate, BigDecimal> rates = new TreeMap<>();

        if (csvResponse == null || csvResponse.trim().isEmpty()) {
            logger.warn("Empty response received from Bundesbank API");
            return rates;
        }

        String[] lines = csvResponse.split("\n");
        boolean dataSection = false;

        for (String line : lines) {
            line = line.trim();
            
            if (line.isEmpty()) {
                continue;
            }

            // Skip header lines until we reach the data
            if (!dataSection) {
                // Look for the data section - typically starts after metadata
                if (line.startsWith("TIME_PERIOD") || line.contains(",OBS_VALUE")) {
                    dataSection = true;
                    continue;
                }
                continue;
            }

            // Parse data lines
            String[] parts = line.split(",|;");
            if (parts.length >= 2) {
                try {
                    String dateStr = parts[0].trim().replace("\"", "");
                    String valueStr = parts[1].trim().replace("\"", "");

                    // Skip if value is empty or marked as not available
                    if (valueStr.isEmpty() || valueStr.equals(".") || valueStr.equals("-")) {
                        continue;
                    }

                    LocalDate date = parseDate(dateStr);
                    BigDecimal rate = new BigDecimal(valueStr);
                    
                    if (date != null && rate.compareTo(BigDecimal.ZERO) > 0) {
                        rates.put(date, rate);
                    }
                } catch (NumberFormatException e) {
                    logger.debug("Skipping invalid rate value in line: {}", line);
                }
            }
        }

        logger.info("Parsed {} exchange rate entries", rates.size());
        return rates;
    }

    /**
     * Parses date strings in various formats.
     */
    private LocalDate parseDate(String dateStr) {
        try {
            // Handle different date formats
            if (dateStr.matches("\\d{4}-\\d{2}-\\d{2}")) {
                return LocalDate.parse(dateStr, DATE_FORMATTER);
            } else if (dateStr.matches("\\d{4}-\\d{2}")) {
                // Monthly data - use first day of month
                return LocalDate.parse(dateStr + "-01", DATE_FORMATTER);
            }
        } catch (Exception e) {
            logger.debug("Failed to parse date: {}", dateStr);
        }
        return null;
    }
}

