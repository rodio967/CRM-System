package com.shift.crm.dto.response;

import java.math.BigDecimal;

public record SellerSumResponse(
        SellerResponse seller,
        BigDecimal totalAmount
) {}
