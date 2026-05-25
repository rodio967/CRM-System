package com.shift.crm.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SellerCreateRequest(
        @NotBlank(message = "Имя продавца не может быть пустым")
        @Size(max = 255)
        String name,

        @NotBlank(message = "Контактная информация обязательна")
        @Size(max = 255)
        String contactInfo
) {}
