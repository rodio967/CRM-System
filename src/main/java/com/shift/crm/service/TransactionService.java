package com.shift.crm.service;

import com.shift.crm.dto.request.TransactionCreateRequest;
import com.shift.crm.dto.response.TransactionResponse;
import com.shift.crm.exception.ResourceNotFoundException;
import com.shift.crm.mapper.TransactionMapper;
import com.shift.crm.model.Seller;
import com.shift.crm.model.Transaction;
import com.shift.crm.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionalRepository;
    private final SellerService sellerService;
    private final TransactionMapper transactionMapper;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<TransactionResponse> findAll() {
        return transactionalRepository.findAll().stream()
                .map(transactionMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TransactionResponse findById(Long id) {
        Transaction transaction = transactionalRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.transaction(id));

        return transactionMapper.toResponse(transaction);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> findBySellerId(Long sellerId) {
        sellerService.getSellerOrTrow(sellerId);

        return transactionalRepository.findAllBySellerIdOrderByTransactionDateAsc(sellerId)
                .stream()
                .map(transactionMapper::toResponse)
                .toList();
    }

    public TransactionResponse create(TransactionCreateRequest request) {
        Seller seller = sellerService.getActiveSellerOrThrow(request.sellerId());
        LocalDateTime date = request.transactionDate() != null
                ? request.transactionDate()
                : LocalDateTime.now(clock);

        Transaction transaction = new Transaction(
                seller,
                request.amount(),
                request.paymentType(),
                date
                );

        Transaction saved = transactionalRepository.save(transaction);
        return transactionMapper.toResponse(saved);
    }


}
