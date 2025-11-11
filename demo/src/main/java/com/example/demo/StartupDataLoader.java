package com.example.demo;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import java.time.LocalDate;

//компонент обеспечит первичное наполнение БД - запросы к центробанку с 01.10.2025 по сегодняшний день
@Component
public class StartupDataLoader {

    private final CurrencyCollectorService collector;
    private final CurrencyRepository repo;

    public StartupDataLoader(CurrencyCollectorService collector, CurrencyRepository repo) {
        this.collector = collector;
        this.repo = repo;
    }

    @PostConstruct
    public void initDatabase() {
        if (repo.count() == 0) {
            System.out.println("[INIT] База пуста, выполняем начальную загрузку с 01.10.2025");
            collector.collectAndSave("https://cbr.ru/currency_base/daily/",
                    LocalDate.of(2025, 10, 1),
                    LocalDate.now());
            collector.updateDailyChanges();
            System.out.println("[INIT] Загрузка завершена");
        } else {
            System.out.println("[INIT] База уже содержит данные, начальная загрузка не требуется");
        }
    }
}
