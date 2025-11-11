package com.example.demo;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class CurrencyQueryService {

    private final CurrencyRepository repo;

    public CurrencyQueryService(CurrencyRepository repo) {
        this.repo = repo;
    }

    public List<CurrencyExchEntity> query(
            LocalDate date,
            List<String> currencyCodes,
            String sortBy,
            boolean ascending) {
        // определяем поле сортировки
        Sort sort = Sort.by(ascending ? Sort.Direction.ASC : Sort.Direction.DESC, mapSortField(sortBy));
        // делаем выборку из БД
        List<CurrencyExchEntity> baseList;
        if (date != null && currencyCodes != null && !currencyCodes.isEmpty()) {
            baseList = repo.findByRateDateAndCurrencyCodeIn(date, currencyCodes, sort);
        } else if (date != null) {
            baseList = repo.findByRateDate(date, sort);
        } else if (currencyCodes != null && !currencyCodes.isEmpty()) {
            baseList = repo.findAll(sort).stream()
                    .filter(e -> currencyCodes.contains(e.getCurrencyCode()))
                    .collect(Collectors.toList());
        } else {
            baseList = repo.findAll(sort);
        }
        // parallelStream для дополнительных фильтров
        ConcurrentLinkedQueue<CurrencyExchEntity> result = new ConcurrentLinkedQueue<>();
        baseList.parallelStream()
                .filter(e -> e.getExchangeRate() > 0) // например, пропускаем только положительные курсы
                .forEach(result::add);
        Stream<CurrencyExchEntity> sortedStream = result.parallelStream();
        Comparator<CurrencyExchEntity> comparator = getComparator(sortBy, ascending);
        return sortedStream
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    public List<CurrencyExchEntity> queryToday(List<String> codes, String sortBy, boolean ascending) {
        return query(LocalDate.now(), codes, sortBy, ascending);
    }

    public List<CurrencyExchEntity> queryAll(String sortBy, boolean ascending) {
        return query(null, null, sortBy, ascending);
    }

    private String mapSortField(String sortBy) {
        return switch (sortBy) {
            case "rate" -> "exchangeRate";
            case "date" -> "rateDate";
            case "code" -> "currencyCode";
            default -> "currencyCode";
        };
    }

    private Comparator<CurrencyExchEntity> getComparator(String sortBy, boolean ascending) {
        Comparator<CurrencyExchEntity> comparator = switch (sortBy) {
            case "rate" -> Comparator.comparing(CurrencyExchEntity::getExchangeRate);
            case "date" -> Comparator.comparing(CurrencyExchEntity::getRateDate);
            case "code" -> Comparator.comparing(CurrencyExchEntity::getCurrencyCode);
            default -> Comparator.comparing(CurrencyExchEntity::getCurrencyCode);
        };
        return ascending ? comparator : comparator.reversed();
    }
}
