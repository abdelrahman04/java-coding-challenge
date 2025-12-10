package com.crewmeister.cmcodingchallenge.config;

import com.crewmeister.cmcodingchallenge.service.ExchangeRateService;
import com.crewmeister.cmcodingchallenge.service.impl.CurrencyServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Initializes the application data on startup.
 * Loads currency definitions and fetches exchange rates from Bundesbank API.
 */
@Component
@Profile("!test") // Don't run during tests
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final CurrencyServiceImpl currencyService;
    private final ExchangeRateService exchangeRateService;

    public DataInitializer(CurrencyServiceImpl currencyService, 
                           ExchangeRateService exchangeRateService) {
        this.currencyService = currencyService;
        this.exchangeRateService = exchangeRateService;
    }

    @Override
    public void run(String... args) {
        logger.info("=== Starting data initialization ===");
        
        try {
            // First initialize currencies
            logger.info("Step 1: Initializing currencies...");
            currencyService.initializeCurrencies();
            
            // Then fetch exchange rates from Bundesbank
            logger.info("Step 2: Fetching exchange rates from Bundesbank API...");
            exchangeRateService.refreshExchangeRates();
            
            logger.info("=== Data initialization completed successfully ===");
            
        } catch (Exception e) {
            logger.error("Error during data initialization: {}", e.getMessage(), e);
            logger.warn("Application will continue but exchange rate data may be incomplete");
        }
    }
}

