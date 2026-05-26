package com.shift.crm.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BestPeriodResponse(
        Long sellerId,
        long periodDays,
        LocalDateTime periodStart,
        LocalDateTime periodEnd,
        int transactionCount,
        BigDecimal totalAmount
) {}
