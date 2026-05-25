package com.shift.crm.dto.request;

import jakarta.validation.constraints.Size;

public record SellerUpdateRequest(
        @Size(min = 1, max = 255, message = "Имя должно быть длиной 1-255")
        String name,

        @Size(min = 1, max = 255, message = "Контактная информация должна быть длиной 1-255")
        String contactInfo
) {}
