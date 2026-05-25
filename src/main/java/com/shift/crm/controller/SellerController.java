package com.shift.crm.controller;

import com.shift.crm.dto.request.SellerCreateRequest;
import com.shift.crm.dto.request.SellerUpdateRequest;
import com.shift.crm.dto.response.SellerResponse;
import com.shift.crm.service.SellerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sellers")
@RequiredArgsConstructor
public class SellerController {

    private final SellerService sellerService;

    @GetMapping
    public ResponseEntity<List<SellerResponse>> getAll() {
        return ResponseEntity.ok(sellerService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SellerResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(sellerService.findById(id));
    }

    @PostMapping
    public ResponseEntity<SellerResponse> create(@Valid @RequestBody SellerCreateRequest request) {
        SellerResponse created = sellerService.create(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SellerResponse> update(@PathVariable Long id, @Valid @RequestBody SellerUpdateRequest request) {

        return ResponseEntity.ok(sellerService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<SellerResponse> delete(@PathVariable Long id) {
        sellerService.delete(id);

        return ResponseEntity.noContent().build();
    }

}
