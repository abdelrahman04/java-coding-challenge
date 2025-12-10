package com.crewmeister.cmcodingchallenge.controller;

import com.crewmeister.cmcodingchallenge.domain.entity.Currency;
import com.crewmeister.cmcodingchallenge.domain.entity.ExchangeRate;
import com.crewmeister.cmcodingchallenge.repository.CurrencyRepository;
import com.crewmeister.cmcodingchallenge.repository.ExchangeRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for ExchangeRateController.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExchangeRateControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CurrencyRepository currencyRepository;

    @Autowired
    private ExchangeRateRepository exchangeRateRepository;

    private Currency usdCurrency;
    private Currency gbpCurrency;
    private LocalDate testDate;

    @BeforeEach
    void setUp() {
        exchangeRateRepository.deleteAll();
        currencyRepository.deleteAll();

        usdCurrency = currencyRepository.save(new Currency("USD", "US Dollar"));
        gbpCurrency = currencyRepository.save(new Currency("GBP", "British Pound Sterling"));
        testDate = LocalDate.of(2024, 1, 15);
    }

    @Nested
    @DisplayName("GET /api/exchange-rates")
    class GetAllExchangeRatesTests {

        @Test
        @DisplayName("Should return all exchange rates")
        void shouldReturnAllExchangeRates() throws Exception {
            // Given
            exchangeRateRepository.save(new ExchangeRate(usdCurrency, testDate, new BigDecimal("1.0850")));
            exchangeRateRepository.save(new ExchangeRate(gbpCurrency, testDate, new BigDecimal("0.8560")));

            // When/Then
            mockMvc.perform(get("/api/exchange-rates")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[*].currencyCode", containsInAnyOrder("USD", "GBP")));
        }

        @Test
        @DisplayName("Should return empty list when no rates")
        void shouldReturnEmptyListWhenNoRates() throws Exception {
            mockMvc.perform(get("/api/exchange-rates")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("GET /api/exchange-rates/date/{date}")
    class GetExchangeRatesByDateTests {

        @Test
        @DisplayName("Should return rates for specific date")
        void shouldReturnRatesForDate() throws Exception {
            // Given
            exchangeRateRepository.save(new ExchangeRate(usdCurrency, testDate, new BigDecimal("1.0850")));
            exchangeRateRepository.save(new ExchangeRate(usdCurrency, testDate.plusDays(1), new BigDecimal("1.0900")));

            // When/Then
            mockMvc.perform(get("/api/exchange-rates/date/{date}", testDate.toString())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].date", is(testDate.toString())));
        }

        @Test
        @DisplayName("Should return bad request for invalid date format")
        void shouldReturnBadRequestForInvalidDate() throws Exception {
            mockMvc.perform(get("/api/exchange-rates/date/{date}", "invalid-date")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/exchange-rates/{currencyCode}/{date}")
    class GetExchangeRateTests {

        @Test
        @DisplayName("Should return rate for currency and date")
        void shouldReturnRateForCurrencyAndDate() throws Exception {
            // Given
            exchangeRateRepository.save(new ExchangeRate(usdCurrency, testDate, new BigDecimal("1.0850")));

            // When/Then
            mockMvc.perform(get("/api/exchange-rates/{currency}/{date}", "USD", testDate.toString())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.currencyCode", is("USD")))
                    .andExpect(jsonPath("$.rate", is(1.085)));
        }

        @Test
        @DisplayName("Should return 404 for unknown currency")
        void shouldReturn404ForUnknownCurrency() throws Exception {
            mockMvc.perform(get("/api/exchange-rates/{currency}/{date}", "XYZ", testDate.toString())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message", containsString("Currency not found")));
        }

        @Test
        @DisplayName("Should return 404 when rate not found for date")
        void shouldReturn404WhenRateNotFound() throws Exception {
            mockMvc.perform(get("/api/exchange-rates/{currency}/{date}", "USD", testDate.toString())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message", containsString("Exchange rate not found")));
        }
    }

    @Nested
    @DisplayName("GET /api/exchange-rates/convert")
    class ConvertToEurTests {

        @Test
        @DisplayName("Should convert amount to EUR")
        void shouldConvertAmountToEur() throws Exception {
            // Given - 1 EUR = 1.0850 USD
            exchangeRateRepository.save(new ExchangeRate(usdCurrency, testDate, new BigDecimal("1.0850")));

            // When/Then - 100 USD / 1.0850 = 92.1659 EUR
            mockMvc.perform(get("/api/exchange-rates/convert")
                            .param("currencyCode", "USD")
                            .param("amount", "100")
                            .param("date", testDate.toString())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sourceCurrency", is("USD")))
                    .andExpect(jsonPath("$.targetCurrency", is("EUR")))
                    .andExpect(jsonPath("$.sourceAmount", is(100)))
                    .andExpect(jsonPath("$.convertedAmount", closeTo(92.1659, 0.001)));
        }

        @Test
        @DisplayName("Should return 404 for unknown currency")
        void shouldReturn404ForUnknownCurrency() throws Exception {
            mockMvc.perform(get("/api/exchange-rates/convert")
                            .param("currencyCode", "XYZ")
                            .param("amount", "100")
                            .param("date", testDate.toString())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 400 for missing parameters")
        void shouldReturn400ForMissingParams() throws Exception {
            mockMvc.perform(get("/api/exchange-rates/convert")
                            .param("currencyCode", "USD")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }
    }
}

