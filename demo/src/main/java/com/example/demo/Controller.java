package com.example.demo;

import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/answer")
public class Controller {

    private final CurrencyQueryService query;

    public Controller(CurrencyQueryService query) {
        this.query = query;
    }

    // эндпоинт с параметрами
    @GetMapping
    public List<CurrencyExchEntity> getCurrencies(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) List<String> codes,
            @RequestParam(defaultValue = "date") String sortBy,
            @RequestParam(defaultValue = "true") boolean asc
    ) {
        LocalDate localDate = null;
        if (date != null && !date.isEmpty()) {
            localDate = LocalDate.parse(date);
        }
        return query.query(localDate, codes, sortBy, asc);
    }

    // эндпоинт для 5 главных валют
    @GetMapping("/main")
    public List<CurrencyExchEntity> getMainCurrencies(
            @RequestParam(required = false) String date,
            @RequestParam(defaultValue = "code") String sortBy,
            @RequestParam(defaultValue = "true") boolean asc
    ) {
        List<String> mainCodes = List.of("EUR", "USD", "CNY", "GBP", "CHF");
        LocalDate localDate = null;
        if (date != null && !date.isEmpty()) {
            localDate = LocalDate.parse(date);
        }
        return query.query(localDate, mainCodes, sortBy, asc);
    }

    // DTO для POST-запроса
    public static class CurrencyQueryRequest {
        private String date; // "YYYY-MM-DD"
        private List<String> currencyCodes;
        private String sortBy;
        private boolean ascending;

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }

        public List<String> getCurrencyCodes() { return currencyCodes; }
        public void setCurrencyCodes(List<String> currencyCodes) { this.currencyCodes = currencyCodes; }

        public String getSortBy() { return sortBy; }
        public void setSortBy(String sortBy) { this.sortBy = sortBy; }

        public boolean isAscending() { return ascending; }
        public void setAscending(boolean ascending) { this.ascending = ascending; }
    }


    // POST-запрос для фильтрации с JSON
    @PostMapping
    public List<CurrencyExchEntity> queryCurrencies(@RequestBody CurrencyQueryRequest request) {
        LocalDate localDate = null;
        if (request.getDate() != null && !request.getDate().isEmpty()) {
            localDate = LocalDate.parse(request.getDate());
        }
        String sortBy = (request.getSortBy() == null || request.getSortBy().isEmpty())
                ? "date" : request.getSortBy();
        boolean ascending = request.isAscending();
        return query.query(localDate, request.getCurrencyCodes(), sortBy, ascending);
    }

    @PostMapping("/main")
    public List<CurrencyExchEntity> queryMainCurrencies(@RequestBody CurrencyQueryRequest request) {
        List<String> mainCodes = List.of("EUR", "USD", "CNY", "GBP", "CHF");
        LocalDate localDate = null;
        if (request.getDate() != null && !request.getDate().isEmpty()) {
            localDate = LocalDate.parse(request.getDate());
        }
        String sortBy = (request.getSortBy() == null || request.getSortBy().isEmpty())
                ? "code" : request.getSortBy();
        boolean ascending = request.isAscending();
        return query.query(localDate, mainCodes, sortBy, ascending);
    }
}
