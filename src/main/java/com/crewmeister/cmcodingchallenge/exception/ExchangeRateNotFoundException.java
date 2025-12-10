package com.crewmeister.cmcodingchallenge.exception;

import java.time.LocalDate;

/**
 * Exception thrown when exchange rate data is not available for the requested date.
 */
public class ExchangeRateNotFoundException extends RuntimeException {

    private final String currencyCode;
    private final LocalDate date;

    public ExchangeRateNotFoundException(String currencyCode, LocalDate date) {
        super(String.format("Exchange rate not found for currency %s on date %s", currencyCode, date));
        this.currencyCode = currencyCode;
        this.date = date;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public LocalDate getDate() {
        return date;
    }
}

