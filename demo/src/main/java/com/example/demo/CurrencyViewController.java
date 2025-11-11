package com.example.demo;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/view")
public class CurrencyViewController {

    private final CurrencyQueryService queryService;

    public CurrencyViewController(CurrencyQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping
    public String viewCurrencies(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) List<String> codes,
            @RequestParam(defaultValue = "date") String sortBy,
            @RequestParam(defaultValue = "true") boolean asc,
            @RequestParam(defaultValue = "0") int page,
            Model model
    ) {
        LocalDate localDate = (date != null && !date.isEmpty()) ? LocalDate.parse(date) : null;
        List<CurrencyExchEntity> all = queryService.query(localDate, codes, sortBy, asc);

        paginateAndRender(model, all, page, sortBy, asc, date);
        return "answer";
    }

    // представление только пяти главных валют
    @GetMapping("/main")
    public String viewMainCurrencies(
            @RequestParam(required = false) String date,
            @RequestParam(defaultValue = "code") String sortBy,
            @RequestParam(defaultValue = "true") boolean asc,
            @RequestParam(defaultValue = "0") int page,
            Model model
    ) {
        List<String> mainCodes = Arrays.asList("EUR", "USD", "CNY", "GBP", "CHF");
        LocalDate localDate = (date != null && !date.isEmpty()) ? LocalDate.parse(date) : null;
        List<CurrencyExchEntity> all = queryService.query(localDate, mainCodes, sortBy, asc);

        paginateAndRender(model, all, page, sortBy, asc, date);
        return "answer_main";
    }

    private void paginateAndRender(Model model, List<CurrencyExchEntity> all, int page, String sortBy, boolean asc, String date) {
        int pageSize = 55;
        int fromIndex = page * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, all.size());
        List<CurrencyExchEntity> pageList = all.subList(fromIndex, toIndex);

        model.addAttribute("currencies", pageList);
        model.addAttribute("page", page);
        model.addAttribute("hasPrev", page > 0);
        model.addAttribute("hasNext", toIndex < all.size());
        model.addAttribute("date", date);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("asc", asc);
    }
}
