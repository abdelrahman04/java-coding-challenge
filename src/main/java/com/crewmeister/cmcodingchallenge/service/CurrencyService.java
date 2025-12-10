package com.crewmeister.cmcodingchallenge.service;

import com.crewmeister.cmcodingchallenge.dto.CurrencyDto;

import java.util.List;

/**
 * Service interface for currency-related operations.
 */
public interface CurrencyService {

    /**
     * Retrieves all available currencies.
     * 
     * @return List of available currencies
     */
    List<CurrencyDto> getAllCurrencies();

    /**
     * Checks if a currency is supported.
     * 
     * @param currencyCode ISO currency code
     * @return true if the currency is supported
     */
    boolean isCurrencySupported(String currencyCode);
}

