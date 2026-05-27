package com.shift.crm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shift.crm.dto.response.TransactionResponse;
import com.shift.crm.model.PaymentType;
import com.shift.crm.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean
    private TransactionService transactionService;

    @Test
    void getAll_returnsList() throws Exception {
        when(transactionService.findAll()).thenReturn(List.of(
                new TransactionResponse(1L, 2L, "Мария",
                        new BigDecimal("100.00"),
                        PaymentType.CARD, LocalDateTime.now())
        ));

        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sellerName").value("Мария"));
    }

    @Test
    void getAll_filteredBySeller() throws Exception {
        when(transactionService.findBySellerId(2L)).thenReturn(List.of());

        mockMvc.perform(get("/api/transactions?sellerId=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(transactionService).findBySellerId(2L);
    }

    @Test
    void create_negativeAmount_returns400() throws Exception {
        String body = """
                {"sellerId": 1, "amount": -10, "paymentType": "CARD"}
                """;
        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'amount')]").exists());
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        TransactionResponse response = new TransactionResponse(
                10L, 1L, "Иван", new BigDecimal("250.00"),
                PaymentType.CASH, LocalDateTime.now());
        when(transactionService.create(any())).thenReturn(response);

        String body = """
                {"sellerId": 1, "amount": 250.00, "paymentType": "CASH"}
                """;
        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10));
    }
}
