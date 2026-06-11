package com.economy.finance.api;

import com.economy.finance.api.dto.DashboardResponse;
import com.economy.finance.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public DashboardResponse load(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(required = false, defaultValue = "principal") String accountPublicKey) {
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Mês deve estar entre 1 e 12");
        }
        return dashboardService.load(year, month, accountPublicKey);
    }
}
