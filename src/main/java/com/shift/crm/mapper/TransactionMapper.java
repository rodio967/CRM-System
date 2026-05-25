package com.shift.crm.mapper;

import com.shift.crm.dto.response.TransactionResponse;
import com.shift.crm.model.Transaction;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

    public TransactionResponse toResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getSeller().getId(),
                transaction.getSeller().getName(),
                transaction.getAmount(),
                transaction.getPaymentType(),
                transaction.getTransactionDate()
        );
    }
}
