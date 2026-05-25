package com.shift.crm.dto.request;

import com.shift.crm.model.PaymentType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionCreateRequest(

        @NotNull(message = "sellerId обязателен")
        Long sellerId,

        @NotNull(message = "amount обязателен")
        @DecimalMin(value = "0.01", message = "Сумма должна быть больше нуля")
        @Digits(integer = 20, fraction = 2, message = "Допустимая точность — 2 знака после запятой")
        BigDecimal amount,

        @NotNull(message = "paymentType обязателен (CASH, CARD или TRANSFER)")
        PaymentType paymentType,

        LocalDateTime transactionDate
) {}
