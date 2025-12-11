package com.crewmeister.cmcodingchallenge.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Data Transfer Object for currency conversion results.
 */
public class ConversionResultDto {

    private String sourceCurrency;
    private String targetCurrency;
    private BigDecimal sourceAmount;
    private BigDecimal convertedAmount;
    private BigDecimal exchangeRate;
    private LocalDate conversionDate;

    public ConversionResultDto() {
    }

    private ConversionResultDto(Builder builder) {
        this.sourceCurrency = builder.sourceCurrency;
        this.targetCurrency = builder.targetCurrency;
        this.sourceAmount = builder.sourceAmount;
        this.convertedAmount = builder.convertedAmount;
        this.exchangeRate = builder.exchangeRate;
        this.conversionDate = builder.conversionDate;
    }

    public String getSourceCurrency() {
        return sourceCurrency;
    }

    public void setSourceCurrency(String sourceCurrency) {
        this.sourceCurrency = sourceCurrency;
    }

    public String getTargetCurrency() {
        return targetCurrency;
    }

    public void setTargetCurrency(String targetCurrency) {
        this.targetCurrency = targetCurrency;
    }

    public BigDecimal getSourceAmount() {
        return sourceAmount;
    }

    public void setSourceAmount(BigDecimal sourceAmount) {
        this.sourceAmount = sourceAmount;
    }

    public BigDecimal getConvertedAmount() {
        return convertedAmount;
    }

    public void setConvertedAmount(BigDecimal convertedAmount) {
        this.convertedAmount = convertedAmount;
    }

    public BigDecimal getExchangeRate() {
        return exchangeRate;
    }

    public void setExchangeRate(BigDecimal exchangeRate) {
        this.exchangeRate = exchangeRate;
    }

    public LocalDate getConversionDate() {
        return conversionDate;
    }

    public void setConversionDate(LocalDate conversionDate) {
        this.conversionDate = conversionDate;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String sourceCurrency;
        private String targetCurrency;
        private BigDecimal sourceAmount;
        private BigDecimal convertedAmount;
        private BigDecimal exchangeRate;
        private LocalDate conversionDate;

        public Builder sourceCurrency(String sourceCurrency) {
            this.sourceCurrency = sourceCurrency;
            return this;
        }

        public Builder targetCurrency(String targetCurrency) {
            this.targetCurrency = targetCurrency;
            return this;
        }

        public Builder sourceAmount(BigDecimal sourceAmount) {
            this.sourceAmount = sourceAmount;
            return this;
        }

        public Builder convertedAmount(BigDecimal convertedAmount) {
            this.convertedAmount = convertedAmount;
            return this;
        }

        public Builder exchangeRate(BigDecimal exchangeRate) {
            this.exchangeRate = exchangeRate;
            return this;
        }

        public Builder conversionDate(LocalDate conversionDate) {
            this.conversionDate = conversionDate;
            return this;
        }

        public ConversionResultDto build() {
            return new ConversionResultDto(this);
        }
    }
}


