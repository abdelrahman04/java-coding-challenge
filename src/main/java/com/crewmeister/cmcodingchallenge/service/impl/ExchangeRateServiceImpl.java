package com.crewmeister.cmcodingchallenge.service.impl;

import com.crewmeister.cmcodingchallenge.client.BundesbankApiClient;
import com.crewmeister.cmcodingchallenge.domain.entity.Currency;
import com.crewmeister.cmcodingchallenge.domain.entity.ExchangeRate;
import com.crewmeister.cmcodingchallenge.dto.ConversionResultDto;
import com.crewmeister.cmcodingchallenge.dto.ExchangeRateDto;
import com.crewmeister.cmcodingchallenge.exception.CurrencyNotFoundException;
import com.crewmeister.cmcodingchallenge.exception.ExchangeRateNotFoundException;
import com.crewmeister.cmcodingchallenge.repository.CurrencyRepository;
import com.crewmeister.cmcodingchallenge.repository.ExchangeRateRepository;
import com.crewmeister.cmcodingchallenge.service.ExchangeRateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of ExchangeRateService.
 * Handles all exchange rate related business logic including
 * fetching, storing, and converting currency amounts.
 */
@Service
@Transactional(readOnly = true)
public class ExchangeRateServiceImpl implements ExchangeRateService {

    private static final Logger logger = LoggerFactory.getLogger(ExchangeRateServiceImpl.class);
    private static final int CONVERSION_SCALE = 4;

    private final ExchangeRateRepository exchangeRateRepository;
    private final CurrencyRepository currencyRepository;
    private final BundesbankApiClient bundesbankApiClient;

    public ExchangeRateServiceImpl(ExchangeRateRepository exchangeRateRepository,
                                    CurrencyRepository currencyRepository,
                                    BundesbankApiClient bundesbankApiClient) {
        this.exchangeRateRepository = exchangeRateRepository;
        this.currencyRepository = currencyRepository;
        this.bundesbankApiClient = bundesbankApiClient;
    }

    @Override
    public List<ExchangeRateDto> getAllExchangeRates() {
        logger.debug("Fetching all exchange rates");
        
        List<ExchangeRate> rates = exchangeRateRepository.findAllOrderByDateDesc();
        
        return rates.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ExchangeRateDto> getExchangeRatesByDate(LocalDate date) {
        logger.debug("Fetching exchange rates for date: {}", date);
        
        if (date == null) {
            throw new IllegalArgumentException("Date cannot be null");
        }
        
        if (date.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Cannot request exchange rates for future dates");
        }
        
        List<ExchangeRate> rates = exchangeRateRepository.findByRateDate(date);
        
        return rates.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public ExchangeRateDto getExchangeRate(String currencyCode, LocalDate date) {
        logger.debug("Fetching exchange rate for {} on {}", currencyCode, date);
        
        validateCurrencyCode(currencyCode);
        validateDate(date);
        
        String normalizedCode = currencyCode.toUpperCase();
        
        ExchangeRate rate = exchangeRateRepository.findByCurrencyCodeAndDate(normalizedCode, date)
                .orElseThrow(() -> new ExchangeRateNotFoundException(normalizedCode, date));
        
        return toDto(rate);
    }

    @Override
    public ConversionResultDto convertToEur(String currencyCode, BigDecimal amount, LocalDate date) {
        logger.debug("Converting {} {} to EUR for date {}", amount, currencyCode, date);
        
        validateCurrencyCode(currencyCode);
        validateDate(date);
        validateAmount(amount);
        
        String normalizedCode = currencyCode.toUpperCase();
        
        ExchangeRate exchangeRate = exchangeRateRepository.findByCurrencyCodeAndDate(normalizedCode, date)
                .orElseThrow(() -> new ExchangeRateNotFoundException(normalizedCode, date));
        
        // The exchange rate represents how many units of foreign currency equals 1 EUR
        // To convert foreign currency to EUR: amount / rate
        BigDecimal eurAmount = amount.divide(exchangeRate.getRate(), CONVERSION_SCALE, RoundingMode.HALF_UP);
        
        return ConversionResultDto.builder()
                .sourceCurrency(normalizedCode)
                .targetCurrency("EUR")
                .sourceAmount(amount)
                .convertedAmount(eurAmount)
                .exchangeRate(exchangeRate.getRate())
                .conversionDate(date)
                .build();
    }

    @Override
    @Transactional
    public void refreshExchangeRates() {
        logger.info("Starting exchange rate refresh from Bundesbank API");
        
        List<Currency> currencies = currencyRepository.findAll();
        int totalRatesAdded = 0;
        
        for (Currency currency : currencies) {
            try {
                Map<LocalDate, BigDecimal> rates = bundesbankApiClient.fetchExchangeRates(currency.getCurrencyCode());
                int ratesAdded = 0;
                
                for (Map.Entry<LocalDate, BigDecimal> entry : rates.entrySet()) {
                    LocalDate date = entry.getKey();
                    BigDecimal rate = entry.getValue();
                    
                    // Only add if not already exists
                    if (!exchangeRateRepository.existsByCurrencyAndRateDate(currency, date)) {
                        ExchangeRate exchangeRate = new ExchangeRate(currency, date, rate);
                        exchangeRateRepository.save(exchangeRate);
                        ratesAdded++;
                    }
                }
                
                totalRatesAdded += ratesAdded;
                logger.debug("Added {} new rates for {}", ratesAdded, currency.getCurrencyCode());
                
            } catch (Exception e) {
                logger.error("Failed to fetch rates for {}: {}", currency.getCurrencyCode(), e.getMessage());
                // Continue with other currencies even if one fails
            }
        }
        
        logger.info("Exchange rate refresh completed. Added {} new rates.", totalRatesAdded);
    }

    private void validateCurrencyCode(String currencyCode) {
        if (currencyCode == null || currencyCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Currency code cannot be null or empty");
        }
        
        String normalizedCode = currencyCode.toUpperCase();
        if (!currencyRepository.existsByCurrencyCode(normalizedCode)) {
            throw new CurrencyNotFoundException(normalizedCode);
        }
    }

    private void validateDate(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("Date cannot be null");
        }
        if (date.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Cannot request exchange rates for future dates");
        }
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
    }

    private ExchangeRateDto toDto(ExchangeRate exchangeRate) {
        Currency currency = exchangeRate.getCurrency();
        return new ExchangeRateDto(
                currency.getCurrencyCode(),
                currency.getCurrencyName(),
                exchangeRate.getRateDate(),
                exchangeRate.getRate()
        );
    }
}


