package com.crewmeister.cmcodingchallenge.service;

import com.crewmeister.cmcodingchallenge.client.BundesbankApiClient;
import com.crewmeister.cmcodingchallenge.domain.entity.Currency;
import com.crewmeister.cmcodingchallenge.domain.entity.ExchangeRate;
import com.crewmeister.cmcodingchallenge.dto.ConversionResultDto;
import com.crewmeister.cmcodingchallenge.dto.ExchangeRateDto;
import com.crewmeister.cmcodingchallenge.exception.CurrencyNotFoundException;
import com.crewmeister.cmcodingchallenge.exception.ExchangeRateNotFoundException;
import com.crewmeister.cmcodingchallenge.repository.CurrencyRepository;
import com.crewmeister.cmcodingchallenge.repository.ExchangeRateRepository;
import com.crewmeister.cmcodingchallenge.service.impl.ExchangeRateServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ExchangeRateServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
class ExchangeRateServiceTest {

    @Mock
    private ExchangeRateRepository exchangeRateRepository;

    @Mock
    private CurrencyRepository currencyRepository;

    @Mock
    private BundesbankApiClient bundesbankApiClient;

    private ExchangeRateServiceImpl exchangeRateService;

    private Currency usdCurrency;
    private Currency gbpCurrency;
    private LocalDate testDate;

    @BeforeEach
    void setUp() {
        exchangeRateService = new ExchangeRateServiceImpl(
                exchangeRateRepository, currencyRepository, bundesbankApiClient);

        usdCurrency = new Currency("USD", "US Dollar");
        gbpCurrency = new Currency("GBP", "British Pound Sterling");
        testDate = LocalDate.of(2024, 1, 15);
    }

    @Nested
    @DisplayName("getAllExchangeRates")
    class GetAllExchangeRatesTests {

        @Test
        @DisplayName("Should return all exchange rates sorted by date")
        void shouldReturnAllExchangeRatesSortedByDate() {
            // Given
            ExchangeRate rate1 = new ExchangeRate(usdCurrency, testDate, new BigDecimal("1.0850"));
            ExchangeRate rate2 = new ExchangeRate(gbpCurrency, testDate, new BigDecimal("0.8560"));
            when(exchangeRateRepository.findAllOrderByDateDesc()).thenReturn(Arrays.asList(rate1, rate2));

            // When
            List<ExchangeRateDto> result = exchangeRateService.getAllExchangeRates();

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getCurrencyCode()).isEqualTo("USD");
            assertThat(result.get(0).getRate()).isEqualByComparingTo(new BigDecimal("1.0850"));
        }

        @Test
        @DisplayName("Should return empty list when no rates exist")
        void shouldReturnEmptyListWhenNoRates() {
            // Given
            when(exchangeRateRepository.findAllOrderByDateDesc()).thenReturn(List.of());

            // When
            List<ExchangeRateDto> result = exchangeRateService.getAllExchangeRates();

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getExchangeRatesByDate")
    class GetExchangeRatesByDateTests {

        @Test
        @DisplayName("Should return rates for specific date")
        void shouldReturnRatesForDate() {
            // Given
            ExchangeRate rate = new ExchangeRate(usdCurrency, testDate, new BigDecimal("1.0850"));
            when(exchangeRateRepository.findByRateDate(testDate)).thenReturn(List.of(rate));

            // When
            List<ExchangeRateDto> result = exchangeRateService.getExchangeRatesByDate(testDate);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getDate()).isEqualTo(testDate);
        }

        @Test
        @DisplayName("Should throw exception for null date")
        void shouldThrowExceptionForNullDate() {
            // When/Then
            assertThatThrownBy(() -> exchangeRateService.getExchangeRatesByDate(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Date cannot be null");
        }

        @Test
        @DisplayName("Should throw exception for future date")
        void shouldThrowExceptionForFutureDate() {
            // Given
            LocalDate futureDate = LocalDate.now().plusDays(1);

            // When/Then
            assertThatThrownBy(() -> exchangeRateService.getExchangeRatesByDate(futureDate))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Cannot request exchange rates for future dates");
        }
    }

    @Nested
    @DisplayName("getExchangeRate")
    class GetExchangeRateTests {

        @Test
        @DisplayName("Should return rate for currency and date")
        void shouldReturnRateForCurrencyAndDate() {
            // Given
            ExchangeRate rate = new ExchangeRate(usdCurrency, testDate, new BigDecimal("1.0850"));
            when(currencyRepository.existsByCurrencyCode("USD")).thenReturn(true);
            when(exchangeRateRepository.findByCurrencyCodeAndDate("USD", testDate))
                    .thenReturn(Optional.of(rate));

            // When
            ExchangeRateDto result = exchangeRateService.getExchangeRate("USD", testDate);

            // Then
            assertThat(result.getCurrencyCode()).isEqualTo("USD");
            assertThat(result.getRate()).isEqualByComparingTo(new BigDecimal("1.0850"));
        }

        @Test
        @DisplayName("Should handle lowercase currency code")
        void shouldHandleLowercaseCurrencyCode() {
            // Given
            ExchangeRate rate = new ExchangeRate(usdCurrency, testDate, new BigDecimal("1.0850"));
            when(currencyRepository.existsByCurrencyCode("USD")).thenReturn(true);
            when(exchangeRateRepository.findByCurrencyCodeAndDate("USD", testDate))
                    .thenReturn(Optional.of(rate));

            // When
            ExchangeRateDto result = exchangeRateService.getExchangeRate("usd", testDate);

            // Then
            assertThat(result.getCurrencyCode()).isEqualTo("USD");
        }

        @Test
        @DisplayName("Should throw CurrencyNotFoundException for unknown currency")
        void shouldThrowExceptionForUnknownCurrency() {
            // Given
            when(currencyRepository.existsByCurrencyCode("XYZ")).thenReturn(false);

            // When/Then
            assertThatThrownBy(() -> exchangeRateService.getExchangeRate("XYZ", testDate))
                    .isInstanceOf(CurrencyNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw ExchangeRateNotFoundException when rate not found")
        void shouldThrowExceptionWhenRateNotFound() {
            // Given
            when(currencyRepository.existsByCurrencyCode("USD")).thenReturn(true);
            when(exchangeRateRepository.findByCurrencyCodeAndDate("USD", testDate))
                    .thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> exchangeRateService.getExchangeRate("USD", testDate))
                    .isInstanceOf(ExchangeRateNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("convertToEur")
    class ConvertToEurTests {

        @Test
        @DisplayName("Should convert foreign currency to EUR correctly")
        void shouldConvertToEurCorrectly() {
            // Given - 1 EUR = 1.0850 USD, so 100 USD = 92.17 EUR (approximately)
            BigDecimal rate = new BigDecimal("1.0850");
            BigDecimal amount = new BigDecimal("100.00");
            ExchangeRate exchangeRate = new ExchangeRate(usdCurrency, testDate, rate);

            when(currencyRepository.existsByCurrencyCode("USD")).thenReturn(true);
            when(exchangeRateRepository.findByCurrencyCodeAndDate("USD", testDate))
                    .thenReturn(Optional.of(exchangeRate));

            // When
            ConversionResultDto result = exchangeRateService.convertToEur("USD", amount, testDate);

            // Then
            assertThat(result.getSourceCurrency()).isEqualTo("USD");
            assertThat(result.getTargetCurrency()).isEqualTo("EUR");
            assertThat(result.getSourceAmount()).isEqualByComparingTo(amount);
            assertThat(result.getExchangeRate()).isEqualByComparingTo(rate);
            // 100 / 1.0850 = 92.1659 (rounded to 4 decimal places)
            assertThat(result.getConvertedAmount()).isEqualByComparingTo(new BigDecimal("92.1659"));
        }

        @Test
        @DisplayName("Should throw exception for null amount")
        void shouldThrowExceptionForNullAmount() {
            // Given
            when(currencyRepository.existsByCurrencyCode("USD")).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> exchangeRateService.convertToEur("USD", null, testDate))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Amount cannot be null");
        }

        @Test
        @DisplayName("Should throw exception for zero amount")
        void shouldThrowExceptionForZeroAmount() {
            // Given
            when(currencyRepository.existsByCurrencyCode("USD")).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> exchangeRateService.convertToEur("USD", BigDecimal.ZERO, testDate))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Amount must be greater than zero");
        }

        @Test
        @DisplayName("Should throw exception for negative amount")
        void shouldThrowExceptionForNegativeAmount() {
            // Given
            when(currencyRepository.existsByCurrencyCode("USD")).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> exchangeRateService.convertToEur("USD", new BigDecimal("-100"), testDate))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Amount must be greater than zero");
        }
    }
}


