package com.example.demo;

import org.springframework.stereotype.Service;
import java.text.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

@Service
public class CurrencyCollectorService {

    private final CurrencyRepository repo;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    public CurrencyCollectorService(CurrencyRepository repo) {
        this.repo = repo;
    }

    // фаза 1: сбор и сохранение данных
    public void collectAndSave(String baseUrl, LocalDate from, LocalDate to) {
        List<LocalDate> dates = getDatesBetween(from, to);
        List<Future<List<CurrencyExchEntity>>> futures = new ArrayList<>();
        // создаём задачу для дат, каждая будет выполняться в своём потоке
        for (LocalDate date : dates)
            futures.add(executor.submit(() -> CurrencyParserJpa.fetchCentrobankRates(baseUrl, date)));
        // получаем результаты из потоков по мере готовности
        for (Future<List<CurrencyExchEntity>> f : futures) {
            try {
                List<CurrencyExchEntity> list = f.get(); // ожидание завершения конкретного потока
                repo.saveAll(list); // сохраняем результаты в БД
                System.out.printf("добавлено %d записей за %s%n", list.size(),
                        list.isEmpty() ? "?" : list.get(0).getRateDate());
            } catch (Exception e) {
                System.err.println("ошибка потока: " + e.getMessage());
            }
        }
    }

    // фаза 2: пересчёт изменений за день
    public void updateDailyChanges() {
        List<CurrencyExchEntity> all = repo.findAll();
        // группируем записи по коду валюты
        Map<String, List<CurrencyExchEntity>> grouped =
                all.stream().collect(Collectors.groupingBy(CurrencyExchEntity::getCurrencyCode));
        List<Future<?>> tasks = new ArrayList<>();
        // для каждой валюты создаём отдельную задачу пересчёта — выполняется в отдельном потоке
        for (var entry : grouped.entrySet()) {
            tasks.add(executor.submit(() -> {
                List<CurrencyExchEntity> list = entry.getValue().stream()
                        .sorted(Comparator.comparing(CurrencyExchEntity::getRateDate))
                        .toList();
                // пересчитываем дневное изменение относительно предыдущей даты
                for (int i = 1; i < list.size(); i++) {
                    CurrencyExchEntity prev = list.get(i - 1);
                    CurrencyExchEntity curr = list.get(i);
                    double diff = curr.getExchangeRate() - prev.getExchangeRate();
                    curr.setDailyChange(diff);
                }
                repo.saveAll(list); // сохраняем обновлённые данные в БД
            }));
        }
        // ожидаем завершения всех потоков
        for (Future<?> task : tasks) {
            try {
                task.get();
            } catch (Exception e) {
                System.err.println("ошибка пересчёта: " + e.getMessage());
            }
        }
        System.out.println("пересчёт изменений завершён");
    }

    public void printAll() {
        List<CurrencyExchEntity> all = repo.findAll();
        CurrencyParserJpa.printCurrencyTable(all);
    }

    private List<LocalDate> getDatesBetween(LocalDate from, LocalDate to) {
        List<LocalDate> list = new ArrayList<>();
        LocalDate current = from;
        while (!current.isAfter(to)) {
            list.add(current);
            current = current.plusDays(1);
        }
        return list;
    }

    public void shutdown() {
        executor.shutdown();
    }
}
