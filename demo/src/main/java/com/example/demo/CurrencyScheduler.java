package com.example.demo;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.Optional;

//планировщик для автоматического обновления БД
@Component
public class CurrencyScheduler {

    private final CurrencyCollectorService collector;
    private final CurrencyRepository repo;

    public CurrencyScheduler(CurrencyCollectorService collector, CurrencyRepository repo) {
        this.collector = collector;
        this.repo = repo;
    }

    //@Scheduled(cron = "0 0 3 * * *") // выполняется каждый день в 03:00 ночи
    @Scheduled(cron = "0 */4 * * * *") // для проверки без долгого ожидания - каждые 4 минуты
    public void scheduledUpdate() {
        System.out.println("[SCHEDULER] Запуск обновления курсов...");
        // находим последнюю дату в базе, а если пусто, стартуем с 2025-10-01
        Optional<LocalDate> lastDateOpt = repo.findAll().stream()
                .map(CurrencyExchEntity::getRateDate)
                .max(LocalDate::compareTo);
        LocalDate from = lastDateOpt.orElse(LocalDate.of(2025, 10, 1)).plusDays(1);
        LocalDate to = LocalDate.now();
        if (!from.isAfter(to)) {
            collector.collectAndSave("https://cbr.ru/currency_base/daily/", from, to);
            collector.updateDailyChanges();
            System.out.println("[SCHEDULER] Обновление завершено: " + from + " - " + to);
        } else {
            System.out.println("[SCHEDULER] Данные актуальны, обновление не требуется");
        }
    }
}

