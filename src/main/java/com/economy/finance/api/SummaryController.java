package com.economy.finance.api;

import com.economy.finance.api.dto.MonthlySummaryResponse;
import com.economy.finance.service.SummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class SummaryController {

    private final SummaryService summaryService;

    @GetMapping("/monthly")
    public MonthlySummaryResponse monthly(
            @RequestParam int year,
            @RequestParam(name = "month") int month,
            @RequestParam(required = false) String accountPublicKey) {
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Mês deve estar entre 1 e 12");
        }
        return summaryService.monthly(year, month, accountPublicKey);
    }
}
