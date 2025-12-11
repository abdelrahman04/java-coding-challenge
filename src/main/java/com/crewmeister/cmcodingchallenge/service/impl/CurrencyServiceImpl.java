package com.crewmeister.cmcodingchallenge.service.impl;

import com.crewmeister.cmcodingchallenge.client.BundesbankApiClient;
import com.crewmeister.cmcodingchallenge.domain.entity.Currency;
import com.crewmeister.cmcodingchallenge.dto.CurrencyDto;
import com.crewmeister.cmcodingchallenge.repository.CurrencyRepository;
import com.crewmeister.cmcodingchallenge.service.CurrencyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of the CurrencyService interface.
 * Manages currency data and provides currency-related operations.
 */
@Service
@Transactional(readOnly = true)
public class CurrencyServiceImpl implements CurrencyService {

    private static final Logger logger = LoggerFactory.getLogger(CurrencyServiceImpl.class);

    private final CurrencyRepository currencyRepository;
    private final BundesbankApiClient bundesbankApiClient;

    public CurrencyServiceImpl(CurrencyRepository currencyRepository, 
                                BundesbankApiClient bundesbankApiClient) {
        this.currencyRepository = currencyRepository;
        this.bundesbankApiClient = bundesbankApiClient;
    }

    @Override
    public List<CurrencyDto> getAllCurrencies() {
        logger.debug("Fetching all available currencies");
        
        List<Currency> currencies = currencyRepository.findAll();
        
        return currencies.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isCurrencySupported(String currencyCode) {
        if (currencyCode == null || currencyCode.trim().isEmpty()) {
            return false;
        }
        return currencyRepository.existsByCurrencyCode(currencyCode.toUpperCase());
    }

    /**
     * Initializes the currency data from the Bundesbank API client.
     * This is called during application startup.
     */
    @Transactional
    public void initializeCurrencies() {
        logger.info("Initializing currency data");
        
        bundesbankApiClient.getSupportedCurrencies().forEach((code, name) -> {
            if (!currencyRepository.existsByCurrencyCode(code)) {
                Currency currency = new Currency(code, name);
                currencyRepository.save(currency);
                logger.debug("Added currency: {} - {}", code, name);
            }
        });
        
        logger.info("Currency initialization completed");
    }

    private CurrencyDto toDto(Currency currency) {
        return new CurrencyDto(currency.getCurrencyCode(), currency.getCurrencyName());
    }
}


