package com.example.demo;

import org.jsoup.*;
import org.jsoup.nodes.*;
import org.jsoup.select.*;
import java.io.*;
import java.text.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.*;

public class CurrencyParserJpa {
    // парсинг курсов валют с сайта ЦБ РФ
    public static List<CurrencyExchEntity> parseCentrobank(String url) {
        List<CurrencyExchEntity> list = new ArrayList<>();
        try {
            LocalDateTime currentDateTime = LocalDateTime.now();;
            LocalDate rateDate = extractRateDateFromUrl(url); //проверяем ссылку на дату
            if (rateDate == null) //если не задана, значит текущая дата
                rateDate = currentDateTime.toLocalDate();;
            Document doc = Jsoup.connect(url).get(); // получаем содержимое сттраницы
            Element table = doc.select("table.data").first(); //таблица с курсами для парсинга
            if (table != null) { //парсинг курсов
                Elements rows = table.select("tr:gt(0)");
                for (Element row : rows) {
                    Elements cells = row.select("td");
                    if (cells.size() >= 5) {
                        String currencyCode = cells.get(1).text();
                        String unitsStr = cells.get(2).text();
                        String currencyName = cells.get(3).text();
                        String rateStr = cells.get(4).text().replace(",", ".");
                        try { // создание записи для добавления в БД (пока что изменение за день остается нулевым_
                            int units = Integer.parseInt(unitsStr);
                            double rate = Double.parseDouble(rateStr);
                            double exchangeRate = rate / units;
                            CurrencyExchEntity entity = new CurrencyExchEntity(
                                    currencyCode, currencyName, exchangeRate,
                                    currentDateTime, rateDate, 0.0
                            );
                            list.add(entity);
                        } catch (NumberFormatException e) {
                            System.err.println("ошибка парсинга числа: " + e.getMessage());
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("ошибка при подключении к сайту: " + e.getMessage());
        }
        return list;
    }
    // извлечение даты из URL - параметр после "UniDbQuery.To="
    private static LocalDate extractRateDateFromUrl(String url) {
        Pattern pattern = Pattern.compile("UniDbQuery\\.To=(\\d{2}\\.\\d{2}\\.\\d{4})");
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            String dateStr = matcher.group(1);
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy");
            return LocalDate.parse(dateStr, fmt);
        }
        return null;
    }
    // метод для выполнения запроса с датой
    public static List<CurrencyExchEntity> fetchCentrobankRates(String baseUrl, LocalDate date) {
        String url;
        if (date != null) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy");
            String formatted = date.format(fmt);
            url = baseUrl + "?UniDbQuery.Posted=True&UniDbQuery.To=" + formatted;
        } else {
            url = baseUrl;
        }
        return parseCentrobank(url);
    }

    public static void printCurrencyTable(List<CurrencyExchEntity> list) {
        if (list.isEmpty()) {
            System.out.println("список валют пуст");
            return;
        }
        System.out.println("=" + "=".repeat(110));
        System.out.printf("%-6s %-25s %-10s %-12s %-10s %s%n",
                "Код", "Название валюты", "Курс", "Дата курса", "Изменение", "Время запроса");
        System.out.println("=" + "=".repeat(110));
        for (CurrencyExchEntity c : list) {
            System.out.printf("%-6s %-25s %-10.4f %-12s %-10.4f %s%n",
                    c.getCurrencyCode(), c.getCurrencyName(), c.getExchangeRate(),
                    c.getRateDate(), c.getDailyChange(), c.getRequestDateTime());
        }
        System.out.println("=" + "=".repeat(110));
        System.out.println("всего валют: " + list.size());
    }
}
