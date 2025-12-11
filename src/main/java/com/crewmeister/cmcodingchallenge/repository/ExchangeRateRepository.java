package com.crewmeister.cmcodingchallenge.repository;

import com.crewmeister.cmcodingchallenge.domain.entity.Currency;
import com.crewmeister.cmcodingchallenge.domain.entity.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for ExchangeRate entity operations.
 */
@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {

    /**
     * Find all exchange rates for a specific currency, ordered by date descending.
     */
    List<ExchangeRate> findByCurrencyOrderByRateDateDesc(Currency currency);

    /**
     * Find exchange rate for a specific currency on a specific date.
     */
    Optional<ExchangeRate> findByCurrencyAndRateDate(Currency currency, LocalDate rateDate);

    /**
     * Find all exchange rates for a specific date.
     */
    List<ExchangeRate> findByRateDate(LocalDate rateDate);

    /**
     * Find exchange rate by currency code and date.
     */
    @Query("SELECT e FROM ExchangeRate e WHERE e.currency.currencyCode = :currencyCode AND e.rateDate = :date")
    Optional<ExchangeRate> findByCurrencyCodeAndDate(
            @Param("currencyCode") String currencyCode, 
            @Param("date") LocalDate date);

    /**
     * Find all exchange rates ordered by date descending.
     */
    @Query("SELECT e FROM ExchangeRate e ORDER BY e.rateDate DESC, e.currency.currencyCode ASC")
    List<ExchangeRate> findAllOrderByDateDesc();

    /**
     * Check if an exchange rate exists for a currency and date.
     */
    boolean existsByCurrencyAndRateDate(Currency currency, LocalDate rateDate);

    /**
     * Find the most recent exchange rate for a currency.
     */
    Optional<ExchangeRate> findFirstByCurrencyOrderByRateDateDesc(Currency currency);
}


