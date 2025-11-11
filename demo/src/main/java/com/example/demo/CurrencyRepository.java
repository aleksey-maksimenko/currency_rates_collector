package com.example.demo;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface CurrencyRepository extends JpaRepository<CurrencyExchEntity, Long> {

    // все записи за конкретную дату
    List<CurrencyExchEntity> findByRateDate(LocalDate rateDate);
    // фильтрация по дате и списку валют
    List<CurrencyExchEntity> findByRateDateAndCurrencyCodeIn(LocalDate rateDate, List<String> codes);
    // методы с возможностью сортировки
    List<CurrencyExchEntity> findByRateDate(LocalDate rateDate, Sort sort);
    List<CurrencyExchEntity> findByRateDateAndCurrencyCodeIn(LocalDate rateDate, List<String> codes, Sort sort);
}
