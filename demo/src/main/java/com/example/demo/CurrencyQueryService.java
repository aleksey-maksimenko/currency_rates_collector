package com.example.demo;

import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.*;

@Service
public class CurrencyQueryService {

    private final CurrencyRepository repo;

    public CurrencyQueryService(CurrencyRepository repo) {
        this.repo = repo;
    }

    // основной метод выборки с фильтрами и сортировкой
    public List<CurrencyExchEntity> query(
            LocalDate date,
            List<String> currencyCodes,
            String sortBy,
            boolean ascending) {

        // получаем все записи из БД (в реальном API можно выполнять более точный запрос)
        List<CurrencyExchEntity> all = repo.findAll();

        // создаём потокобезопасную структуру для сбора результатов
        ConcurrentLinkedQueue<CurrencyExchEntity> result = new ConcurrentLinkedQueue<>();

        // parallelStream — для ускорения фильтрации
        all.parallelStream()
                .filter(e -> {
                    // фильтр по дате (если date != null, сравниваем по LocalDate)
                    if (date != null && (e.getRateDate() == null || !isSameDay(e.getRateDate(), date)))
                        return false;
                    // фильтр по валютам (если задан список кодов)
                    if (currencyCodes != null && !currencyCodes.isEmpty()
                            && !currencyCodes.contains(e.getCurrencyCode()))
                        return false;
                    return true;
                })
                .forEach(result::add); // безопасно добавляем в ConcurrentLinkedQueue

        // теперь сортировка (параллельно)
        Stream<CurrencyExchEntity> sortedStream = result.parallelStream();

        Comparator<CurrencyExchEntity> comparator = switch (sortBy) {
            case "rate" -> Comparator.comparing(CurrencyExchEntity::getExchangeRate);
            case "date" -> Comparator.comparing(CurrencyExchEntity::getRateDate);
            case "code" -> Comparator.comparing(CurrencyExchEntity::getCurrencyCode);
            default -> Comparator.comparing(CurrencyExchEntity::getCurrencyCode);
        };

        if (!ascending)
            comparator = comparator.reversed();

        return sortedStream
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    // перегрузка — если дата не указана, использовать сегодняшнюю
    public List<CurrencyExchEntity> queryToday(List<String> codes, String sortBy, boolean ascending) {
        return query(LocalDate.now(), codes, sortBy, ascending);
    }

    // перегрузка — все записи
    public List<CurrencyExchEntity> queryAll(String sortBy, boolean ascending) {
        return query(null, null, sortBy, ascending);
    }

    // утилита сравнения двух LocalDate
    private boolean isSameDay(LocalDate d1, LocalDate d2) {
        if (d1 == null || d2 == null) return false;
        return d1.isEqual(d2);
    }
}
