package com.crewmeister.cmcodingchallenge.controller;

import com.crewmeister.cmcodingchallenge.dto.CurrencyDto;
import com.crewmeister.cmcodingchallenge.service.CurrencyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for currency-related endpoints.
 * Provides API access to available currencies in the system.
 */
@RestController
@RequestMapping("/api/currencies")
public class CurrencyController {

    private static final Logger logger = LoggerFactory.getLogger(CurrencyController.class);

    private final CurrencyService currencyService;

    public CurrencyController(CurrencyService currencyService) {
        this.currencyService = currencyService;
    }

    /**
     * Get all available currencies.
     * 
     * @return List of all supported currencies with their codes and names
     */
    @GetMapping
    public ResponseEntity<List<CurrencyDto>> getAllCurrencies() {
        logger.info("GET /api/currencies - Fetching all available currencies");
        
        List<CurrencyDto> currencies = currencyService.getAllCurrencies();
        
        logger.info("Returning {} currencies", currencies.size());
        return ResponseEntity.ok(currencies);
    }
}

