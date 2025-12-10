package com.crewmeister.cmcodingchallenge.exception;

/**
 * Exception thrown when a requested currency is not found in the system.
 */
public class CurrencyNotFoundException extends RuntimeException {

    private final String currencyCode;

    public CurrencyNotFoundException(String currencyCode) {
        super(String.format("Currency not found: %s", currencyCode));
        this.currencyCode = currencyCode;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }
}

