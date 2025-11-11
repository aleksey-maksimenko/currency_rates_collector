package com.example.demo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Assertions;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest
class DemoApplicationTests {

    @Autowired
    private CurrencyCollectorService collector;
    @Autowired
    private CurrencyRepository repo;
    @Autowired
    private CurrencyQueryService query;

    //тест по заполнению БД - берем три дня
    @Test
    @Order(1)
    void testCollector() {
        // задаём диапазон дат
        LocalDate from = LocalDate.of(2025, 10, 1);
        LocalDate to = LocalDate.of(2025, 10, 3);
        collector.collectAndSave("https://cbr.ru/currency_base/daily/", from, to);
        collector.updateDailyChanges();
        List<CurrencyExchEntity> all = repo.findAll();
        // ожидаем 3 дня * 55 валют = 165 записей
        Assertions.assertEquals(165, all.size(), "Некорректное количество записей в БД");
        // проверяем наличие ключевых валют
        boolean hasUSD = all.stream().anyMatch(e -> e.getCurrencyCode().equals("USD"));
        boolean hasEUR = all.stream().anyMatch(e -> e.getCurrencyCode().equals("EUR"));
        Assertions.assertTrue(hasUSD, "Отсутствует валюта USD");
        Assertions.assertTrue(hasEUR, "Отсутствует валюта EUR");

        LocalDate targetDate = LocalDate.of(2025, 10, 2);
        CurrencyExchEntity usd = all.stream()
                .filter(e -> e.getCurrencyCode().equals("USD") && e.getRateDate().equals(targetDate))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Не найдена запись USD за 2025-10-02"));
        CurrencyExchEntity eur = all.stream()
                .filter(e -> e.getCurrencyCode().equals("EUR") && e.getRateDate().equals(targetDate))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Не найдена запись EUR за 2025-10-02"));
        // проверка курса и изменения
        Assertions.assertEquals(81.4967, usd.getExchangeRate(), 0.0001, "Некорректный курс USD");
        Assertions.assertEquals(-1.1117, usd.getDailyChange(), 0.0001, "Некорректное изменение курса USD");
        Assertions.assertEquals(95.6382, eur.getExchangeRate(), 0.0001, "Некорректный курс EUR");
        Assertions.assertEquals(-1.2262, eur.getDailyChange(), 0.0001, "Некорректное изменение курса EUR");
    }

    // вспомогательный метод для наполнения БД при вызове тестов с запросами
    private void ensureDbPopulated() {
        if (repo.count() == 0) {
            LocalDate from = LocalDate.of(2025, 10, 1);
            LocalDate to = LocalDate.of(2025, 10, 3);
            collector.collectAndSave("https://cbr.ru/currency_base/daily/", from, to);
            collector.updateDailyChanges();
        }
    }

    //получение всех записей
    @Test
    @Order(2)
    void testQueryAllSortedByDateAsc() {
        ensureDbPopulated();
        List<CurrencyExchEntity> list = query.queryAll("date", true);
        Assertions.assertEquals(165, list.size(), "Некорректное количество записей");
        Assertions.assertEquals(LocalDate.of(2025, 10, 1), list.get(0).getRateDate(), "Первая дата неверна");
        Assertions.assertEquals(LocalDate.of(2025, 10, 3), list.get(list.size() - 1).getRateDate(), "Последняя дата неверна");
    }
    // получение всех записей с сортировкой по коду валют
    @Test
    @Order(3)
    void testQueryAllSortedByCodeAsc() {
        ensureDbPopulated();
        List<CurrencyExchEntity> list = query.queryAll("code", true);
        Assertions.assertEquals(165, list.size());
        Assertions.assertEquals("AED", list.get(0).getCurrencyCode());
        Assertions.assertEquals("ZAR", list.get(list.size() - 1).getCurrencyCode());
    }
    // получение всех записей с обратной сортировкой по коду валют
    @Test
    void testQueryAllSortedByCodeDesc() {
        ensureDbPopulated();
        List<CurrencyExchEntity> list = query.queryAll("code", false);
        Assertions.assertEquals(165, list.size());
        Assertions.assertEquals("ZAR", list.get(0).getCurrencyCode());
        Assertions.assertEquals("AED", list.get(list.size() - 1).getCurrencyCode());
    }
    // запрос всех курсов на конкретную дату
    @Test
    void testQueryByDate() {
        ensureDbPopulated();
        LocalDate date = LocalDate.of(2025, 10, 2);
        List<CurrencyExchEntity> list = query.query(date, null, "rate", false);
        Assertions.assertEquals(55, list.size(), "Количество записей за дату неверно");
        CurrencyExchEntity usd = list.stream().filter(e -> e.getCurrencyCode().equals("USD")).findFirst()
                .orElseThrow(() -> new AssertionError("USD не найден"));
        //проверяем значения курса и изменений для USD
        Assertions.assertEquals(81.4967, usd.getExchangeRate(), 0.0001);
        Assertions.assertEquals(-1.1117, usd.getDailyChange(), 0.0001);
    }
    //запрос по конкретной дате для стека валют
    @Test
    void testQueryByDateAndGroup() {
        ensureDbPopulated();
        LocalDate date = LocalDate.of(2025, 10, 2);
        List<String> codes = List.of("USD", "EUR", "CNY"); //три валюты для проверки
        List<CurrencyExchEntity> list = query.query(date, codes, "code", false);
        Assertions.assertEquals(3, list.size(), "Количество записей группы неверно");
        Assertions.assertEquals("USD", list.get(0).getCurrencyCode());
        Assertions.assertEquals("EUR", list.get(1).getCurrencyCode());
        Assertions.assertEquals("CNY", list.get(2).getCurrencyCode());
    }
    // курсы группы валют с сортировкой
    @Test
    void testQueryByDateAndGroupRateAsc() {
        ensureDbPopulated();
        LocalDate date = LocalDate.of(2025, 10, 2);
        List<String> codes = List.of("USD", "EUR", "CNY");
        List<CurrencyExchEntity> list = query.query(date, codes, "rate", true);
        Assertions.assertEquals(3, list.size());
        Assertions.assertEquals("CNY", list.get(0).getCurrencyCode());
        Assertions.assertEquals("USD", list.get(1).getCurrencyCode());
        Assertions.assertEquals("EUR", list.get(2).getCurrencyCode());
    }
    //все записи по USD (история курсов в течение времени)
    @Test
    void testQueryAllUsd() {
        ensureDbPopulated();
        List<String> codes = List.of("USD");
        List<CurrencyExchEntity> list = query.query(null, codes, "date", true);
        Assertions.assertEquals(3, list.size(), "Количество записей USD неверно");
        Assertions.assertEquals(82.6084, list.get(0).getExchangeRate(), 0.0001);
        Assertions.assertEquals(81.4967, list.get(1).getExchangeRate(), 0.0001);
        Assertions.assertEquals(81.0085, list.get(2).getExchangeRate(), 0.0001);
    }
    // курсы USD с обратной сортровкой по значению курсов
    @Test
    void testQueryAllUsdRateDesc() {
        ensureDbPopulated();
        List<String> codes = List.of("USD");
        List<CurrencyExchEntity> list = query.query(null, codes, "rate", false);
        Assertions.assertEquals(3, list.size());
        Assertions.assertEquals(82.6084, list.get(0).getExchangeRate(), 0.0001);
        Assertions.assertEquals(81.0085, list.get(2).getExchangeRate(), 0.0001);
    }
}