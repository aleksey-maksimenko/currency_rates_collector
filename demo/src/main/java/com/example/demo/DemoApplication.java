package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.context.*;
import java.util.*;

@SpringBootApplication
public class DemoApplication {

	public static void main(String[] args) {
        //SpringApplication.run(DemoApplication.class, args);
        //System.out.println("ok");
        /*
        String baseUrl = "https://www.cbr.ru/currency_base/daily/";

        System.out.println("=== Текущие курсы валют ===");
        List<CurrencyExchRating> currentRates = CurrencyParser.fetchCentrobankRates(baseUrl);
        CurrencyParser.printCurrencyTable(currentRates);

        System.out.println("\n=== Курсы валют на конкретную дату ===");
        List<CurrencyExchRating> historicalRates = CurrencyParser.fetchCentrobankRates(baseUrl, "07.11.2025");
        CurrencyParser.printCurrencyTable(historicalRates);

        System.out.println("\n=== Курсы валют на другую дату ===");
        List<CurrencyExchRating> anotherDateRates = CurrencyParser.fetchCentrobankRates(baseUrl, "01.11.2024");
        CurrencyParser.printCurrencyTable(anotherDateRates);

        CurrencyCollectorService collector = new CurrencyCollectorService(4);

        // Пример: собираем курсы за 3 дня (даты должны быть в формате "dd.MM.yyyy")
        String from = "01.11.2025";
        String to = "03.11.2025";

        List<CurrencyExchRating> result = collector.collectRates(from, to);

        System.out.println("\n=== Итоговый результат ===");
        result.stream()
                .sorted(Comparator.comparing(CurrencyExchRating::getRateDate)
                        .thenComparing(CurrencyExchRating::getCurrencyCode))
                .forEach(System.out::println);
        System.out.printf("%nВсего записей: %d%n", result.size());
        collector.shutdown();
        *
         */
        ConfigurableApplicationContext ctx = SpringApplication.run(DemoApplication.class, args);
        System.out.println("ok");

        CurrencyCollectorService collector = ctx.getBean(CurrencyCollectorService.class);
        //String from = "01.10.2025";
        //String to = "15.10.2025";
        LocalDate from = LocalDate.of(2025, 10, 1);
        LocalDate to = LocalDate.of(2025, 10, 15);
        collector.collectAndSave("https://cbr.ru/currency_base/daily/", from, to);
        collector.updateDailyChanges(); // фаза 2

        System.out.println("\n=== Итоговый результат ===");
        collector.printAll();
        collector.shutdown();

        CurrencyQueryService query = ctx.getBean(CurrencyQueryService.class);

// 1) получить все записи, отсортированные по коду по возрастанию
        List<CurrencyExchEntity> allByCodeAsc = query.queryAll("code", true);
        System.out.println("\n\n== все записи, сортировка по коду asc ==");
        allByCodeAsc.forEach(System.out::println);

// 2) получить записи за конкретную дату (если не задано — сегодня), сортировка по курсу desc
        LocalDate date = LocalDate.of(2025, 10, 3);
        List<CurrencyExchEntity> byDateRateDesc = query.query(date, null, "rate", false);
        System.out.println("== записи за " + date + ", сортировка по курсу desc ==");
        byDateRateDesc.forEach(System.out::println);

// 3) получить записи для конкретной валюты (или списка валют), сортировка по дате asc
        List<String> codes = List.of("USD");
        List<CurrencyExchEntity> usdHistory = query.query(null, codes, "date", true);
        System.out.println("== все записи для USD (по дате asc) ==");
        usdHistory.forEach(System.out::println);

// 4) записи за конкретную дату для нескольких валют, сортировка по коду desc
        List<String> codes2 = List.of("USD", "EUR", "GBP");
        List<CurrencyExchEntity> subset = query.query(date, codes2, "code", false);
        System.out.println("== записи за " + date + " для USD,EUR,GBP (по коду desc) ==");
        subset.forEach(System.out::println);

        // 5) быстрый вызов — сегодня для списка валют, сортировка по изменению (dailyChange) — можно через "rate" или добавить поддержку "change"
        // пример использования queryToday
        List<CurrencyExchEntity> todayUsd = query.queryToday(List.of("USD", "EUR"), "rate", false);
        System.out.println("== сегодня USD/EUR по курсу desc ==");
        todayUsd.forEach(System.out::println);
	}

}
