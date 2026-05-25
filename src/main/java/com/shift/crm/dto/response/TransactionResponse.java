package com.shift.crm.dto.response;

import com.shift.crm.model.PaymentType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
        Long id,
        Long sellerId,
        String sellerName,
        BigDecimal amount,
        PaymentType paymentType,
        LocalDateTime transactionDate
) {}
