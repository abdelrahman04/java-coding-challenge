package com.crewmeister.cmcodingchallenge.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Data Transfer Object for exchange rate information.
 */
public class ExchangeRateDto {

    private String currencyCode;
    private String currencyName;
    private LocalDate date;
    private BigDecimal rate;

    public ExchangeRateDto() {
    }

    public ExchangeRateDto(String currencyCode, String currencyName, LocalDate date, BigDecimal rate) {
        this.currencyCode = currencyCode;
        this.currencyName = currencyName;
        this.date = date;
        this.rate = rate;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getCurrencyName() {
        return currencyName;
    }

    public void setCurrencyName(String currencyName) {
        this.currencyName = currencyName;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
    }
}

