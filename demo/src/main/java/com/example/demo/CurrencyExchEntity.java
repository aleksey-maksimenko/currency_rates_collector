package com.example.demo;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

//класс-модель для организации таблицы БД с курсами валют
@Entity
@Table(name = "currency_exchange")
public class CurrencyExchEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String currencyCode; // код валюты
    private String currencyName; // название
    private double exchangeRate; // курс к рублю
    private LocalDateTime requestDateTime; // дата запроса
    private LocalDate rateDate; // дата курса
    private double dailyChange; // изменение за день

    public CurrencyExchEntity() {}

    public CurrencyExchEntity(String currencyCode, String currencyName, double exchangeRate,
                              LocalDateTime requestDateTime, LocalDate rateDate, double dailyChange) {
        this.currencyCode = currencyCode;
        this.currencyName = currencyName;
        this.exchangeRate = exchangeRate;
        this.requestDateTime = requestDateTime;
        this.rateDate = rateDate;
        this.dailyChange = dailyChange;
    }

    public String getCurrencyCode() { return currencyCode; }
    public String getCurrencyName() { return currencyName; }
    public double getExchangeRate() { return exchangeRate; }
    public LocalDateTime getRequestDateTime() { return requestDateTime; }
    public LocalDate getRateDate() { return rateDate; }
    public double getDailyChange() { return dailyChange; }

    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    public void setCurrencyName(String currencyName) { this.currencyName = currencyName; }
    public void setExchangeRate(double exchangeRate) { this.exchangeRate = exchangeRate; }
    public void setRequestDateTime(LocalDateTime requestDateTime) { this.requestDateTime = requestDateTime; }
    public void setRateDate(LocalDate rateDate) { this.rateDate = rateDate; }
    public void setDailyChange(double dailyChange) { this.dailyChange = dailyChange; }

    @Override
    public String toString() {
        //return String.format("%-6s %-30s %-10.4f %-12s %-8.4f %s",
        //        currencyCode, currencyName, exchangeRate, rateDate, dailyChange, requestDateTime);
        return String.format("%-6s %-30s %-10.4f %-12s %-8.4f",
                currencyCode, currencyName, exchangeRate, rateDate, dailyChange);
    }
}
