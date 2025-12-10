package com.crewmeister.cmcodingchallenge.service;

import com.crewmeister.cmcodingchallenge.client.BundesbankApiClient;
import com.crewmeister.cmcodingchallenge.domain.entity.Currency;
import com.crewmeister.cmcodingchallenge.dto.CurrencyDto;
import com.crewmeister.cmcodingchallenge.repository.CurrencyRepository;
import com.crewmeister.cmcodingchallenge.service.impl.CurrencyServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CurrencyServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
class CurrencyServiceTest {

    @Mock
    private CurrencyRepository currencyRepository;

    @Mock
    private BundesbankApiClient bundesbankApiClient;

    private CurrencyServiceImpl currencyService;

    @BeforeEach
    void setUp() {
        currencyService = new CurrencyServiceImpl(currencyRepository, bundesbankApiClient);
    }

    @Test
    @DisplayName("Should return all currencies from repository")
    void getAllCurrencies_ShouldReturnAllCurrencies() {
        // Given
        Currency usd = new Currency("USD", "US Dollar");
        Currency gbp = new Currency("GBP", "British Pound Sterling");
        when(currencyRepository.findAll()).thenReturn(Arrays.asList(usd, gbp));

        // When
        List<CurrencyDto> result = currencyService.getAllCurrencies();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getCode()).isEqualTo("USD");
        assertThat(result.get(0).getName()).isEqualTo("US Dollar");
        assertThat(result.get(1).getCode()).isEqualTo("GBP");
        verify(currencyRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should return empty list when no currencies exist")
    void getAllCurrencies_WhenNoCurrencies_ShouldReturnEmptyList() {
        // Given
        when(currencyRepository.findAll()).thenReturn(List.of());

        // When
        List<CurrencyDto> result = currencyService.getAllCurrencies();

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return true for supported currency")
    void isCurrencySupported_WhenCurrencyExists_ShouldReturnTrue() {
        // Given
        when(currencyRepository.existsByCurrencyCode("USD")).thenReturn(true);

        // When
        boolean result = currencyService.isCurrencySupported("USD");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return true for lowercase currency code")
    void isCurrencySupported_WhenLowercase_ShouldNormalize() {
        // Given
        when(currencyRepository.existsByCurrencyCode("USD")).thenReturn(true);

        // When
        boolean result = currencyService.isCurrencySupported("usd");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return false for unsupported currency")
    void isCurrencySupported_WhenCurrencyNotExists_ShouldReturnFalse() {
        // Given
        when(currencyRepository.existsByCurrencyCode("XYZ")).thenReturn(false);

        // When
        boolean result = currencyService.isCurrencySupported("XYZ");

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return false for null currency code")
    void isCurrencySupported_WhenNull_ShouldReturnFalse() {
        // When
        boolean result = currencyService.isCurrencySupported(null);

        // Then
        assertThat(result).isFalse();
        verifyNoInteractions(currencyRepository);
    }

    @Test
    @DisplayName("Should return false for empty currency code")
    void isCurrencySupported_WhenEmpty_ShouldReturnFalse() {
        // When
        boolean result = currencyService.isCurrencySupported("");

        // Then
        assertThat(result).isFalse();
        verifyNoInteractions(currencyRepository);
    }
}

