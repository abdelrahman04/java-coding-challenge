package com.crewmeister.cmcodingchallenge.service;

import com.crewmeister.cmcodingchallenge.dto.ConversionResultDto;
import com.crewmeister.cmcodingchallenge.dto.ExchangeRateDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Service interface for exchange rate operations.
 */
public interface ExchangeRateService {

    /**
     * Retrieves all available exchange rates.
     * 
     * @return List of all exchange rates sorted by date descending
     */
    List<ExchangeRateDto> getAllExchangeRates();

    /**
     * Retrieves all exchange rates for a specific date.
     * 
     * @param date The date to get rates for
     * @return List of exchange rates for the given date
     */
    List<ExchangeRateDto> getExchangeRatesByDate(LocalDate date);

    /**
     * Retrieves the exchange rate for a specific currency on a specific date.
     * 
     * @param currencyCode ISO currency code
     * @param date The date to get the rate for
     * @return Exchange rate information
     */
    ExchangeRateDto getExchangeRate(String currencyCode, LocalDate date);

    /**
     * Converts an amount from a foreign currency to EUR.
     * 
     * @param currencyCode Source currency code
     * @param amount Amount to convert
     * @param date Date for the exchange rate to use
     * @return Conversion result with details
     */
    ConversionResultDto convertToEur(String currencyCode, BigDecimal amount, LocalDate date);

    /**
     * Refreshes exchange rate data from the external API.
     * Called during startup and can be triggered manually.
     */
    void refreshExchangeRates();
}


