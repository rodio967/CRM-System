package com.shift.crm.dto.response;

import com.shift.crm.model.PeriodType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductiveSellerResponse(
        SellerResponse seller,
        BigDecimal totalAmount,
        PeriodType periodType,
        LocalDateTime periodStart,
        LocalDateTime periodEnd
) {}
