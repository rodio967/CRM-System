package com.shift.crm.service;

import com.shift.crm.dto.request.SellerCreateRequest;
import com.shift.crm.dto.request.SellerUpdateRequest;
import com.shift.crm.dto.response.SellerResponse;
import com.shift.crm.exception.ResourceNotFoundException;
import com.shift.crm.mapper.SellerMapper;
import com.shift.crm.model.Seller;
import com.shift.crm.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class SellerService {

    private final SellerRepository sellerRepository;
    private final SellerMapper sellerMapper;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<SellerResponse> findAll() {
        return sellerRepository.findAllByDeletedFalse().stream()
                .map(sellerMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SellerResponse findById(Long id) {
        return sellerMapper.toResponse(getActiveSellerOrThrow(id));
    }


    public SellerResponse create(SellerCreateRequest request) {
        Seller seller = new Seller(request.name(),
                request.contactInfo(),
                LocalDateTime.now(clock)
        );

        Seller saved = sellerRepository.save(seller);
        return sellerMapper.toResponse(saved);
    }


    public SellerResponse update(Long id, SellerUpdateRequest request) {
        Seller seller = getActiveSellerOrThrow(id);

        if (request.name() != null) seller.setName(request.name());
        if (request.contactInfo() != null) seller.setContactInfo(request.contactInfo());

        return sellerMapper.toResponse(seller);
    }

    // soft delete
    @Transactional
    public void delete(Long id) {
        Seller seller = getActiveSellerOrThrow(id);
        seller.markDeleted();
    }



    @Transactional(readOnly = true)
    public Seller getActiveSellerOrThrow(Long id) {
        return sellerRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> ResourceNotFoundException.seller(id));
    }

    @Transactional(readOnly = true)
    public Seller getSellerOrThrow(Long id) {
        return sellerRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.seller(id));
    }


}
