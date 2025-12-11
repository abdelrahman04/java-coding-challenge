package com.crewmeister.cmcodingchallenge.repository;

import com.crewmeister.cmcodingchallenge.domain.entity.Currency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Currency entity operations.
 */
@Repository
public interface CurrencyRepository extends JpaRepository<Currency, String> {

    /**
     * Find a currency by its ISO code (case-insensitive).
     */
    Optional<Currency> findByCurrencyCodeIgnoreCase(String currencyCode);

    /**
     * Check if a currency exists by its code.
     */
    boolean existsByCurrencyCode(String currencyCode);
}


