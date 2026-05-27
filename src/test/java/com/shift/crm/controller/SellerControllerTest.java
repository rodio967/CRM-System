package com.shift.crm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shift.crm.dto.request.SellerCreateRequest;
import com.shift.crm.dto.response.SellerResponse;
import com.shift.crm.exception.ResourceNotFoundException;
import com.shift.crm.service.SellerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SellerController.class)
class SellerControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean
    private SellerService sellerService;

    @Test
    void getAll_returnsList() throws Exception {
        when(sellerService.findAll()).thenReturn(List.of(
                new SellerResponse(1L, "Иван", "ivan@x", LocalDateTime.now())
        ));

        mockMvc.perform(get("/api/sellers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Иван"));
    }

    @Test
    void getById_missing_returns404() throws Exception {
        when(sellerService.findById(100L)).thenThrow(ResourceNotFoundException.seller(100L));

        mockMvc.perform(get("/api/sellers/100"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("100")));
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        SellerResponse response = new SellerResponse(7L, "Иван", "ivan@example.com", LocalDateTime.now());
        when(sellerService.create(any())).thenReturn(response);

        SellerCreateRequest request = new SellerCreateRequest("Иван", "ivan@example.com");

        mockMvc.perform(post("/api/sellers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7));
    }

    @Test
    void create_blankName_returns400WithFieldError() throws Exception {
        String body = """
                {"name": "", "contactInfo": "ivan@x"}
                """;

        mockMvc.perform(post("/api/sellers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
    }

    @Test
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/sellers/1"))
                .andExpect(status().isNoContent());
        verify(sellerService).delete(eq(1L));
    }
}
