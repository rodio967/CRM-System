package com.shift.crm.controller;

import com.shift.crm.dto.response.BestPeriodResponse;
import com.shift.crm.dto.response.ProductiveSellerResponse;
import com.shift.crm.dto.response.SellerSumResponse;
import com.shift.crm.model.PeriodType;
import com.shift.crm.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/most-productive")
    public ResponseEntity<List<ProductiveSellerResponse>> mostProductive(@RequestParam PeriodType period) {
        return ResponseEntity.ok(analyticsService.findMostProductiveSellers(period));
    }

    @GetMapping("/below-threshold")
    public ResponseEntity<List<SellerSumResponse>> sellersBelowThreshold(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam BigDecimal threshold) {

        return ResponseEntity.ok(analyticsService.findSellersWithSumLessThan(from, to, threshold));
    }

    @GetMapping("/best-period")
    public ResponseEntity<BestPeriodResponse> bestPeriod(@RequestParam Long sellerId,
                                                         @RequestParam Long periodDays) {
        Optional<BestPeriodResponse> bestPeriod = analyticsService.findBestPeriod(sellerId, periodDays);
        if (bestPeriod.isEmpty()) return ResponseEntity.noContent().build();

        return ResponseEntity.ok(bestPeriod.get());

    }




}
