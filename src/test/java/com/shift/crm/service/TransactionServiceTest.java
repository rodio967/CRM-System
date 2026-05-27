package com.shift.crm.service;

import com.shift.crm.dto.request.TransactionCreateRequest;
import com.shift.crm.dto.response.TransactionResponse;
import com.shift.crm.exception.ResourceNotFoundException;
import com.shift.crm.mapper.TransactionMapper;
import com.shift.crm.model.PaymentType;
import com.shift.crm.model.Seller;
import com.shift.crm.model.Transaction;
import com.shift.crm.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private SellerService sellerService;

    private TransactionMapper mapper;
    private Clock clock;
    private TransactionService service;

    @BeforeEach
    void setUp() {
        mapper = new TransactionMapper();
        clock = Clock.fixed(Instant.parse("2026-05-24T10:00:00Z"), ZoneId.of("UTC"));
        service = new TransactionService(transactionRepository, sellerService, mapper, clock);
    }

    private Seller seller() {
        Seller seller = new Seller("Иван", "ivan@example.com", LocalDateTime.now(clock));
        try {
            var f = Seller.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(seller, 1L);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        return seller;
    }

    @Test
    void create_usesProvidedDate() {
        Seller s = seller();
        when(sellerService.getActiveSellerOrThrow(1L)).thenReturn(s);
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LocalDateTime date = LocalDateTime.of(2026, 5, 10, 12, 0);
        TransactionCreateRequest request = new TransactionCreateRequest(
                1L, new BigDecimal("100.00"), PaymentType.CARD, date);

        TransactionResponse response = service.create(request);

        verify(transactionRepository).save(captor.capture());
        assertThat(captor.getValue().getTransactionDate()).isEqualTo(date);
        assertThat(response.amount()).isEqualByComparingTo("100.00");
    }

    @Test
    void create_fillsTransactionDateFromClockWhenMissing() {
        Seller s = seller();
        when(sellerService.getActiveSellerOrThrow(1L)).thenReturn(s);
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TransactionCreateRequest request = new TransactionCreateRequest(
                1L, new BigDecimal("50.00"), PaymentType.CASH, null);

        service.create(request);
        verify(transactionRepository).save(captor.capture());
        assertThat(captor.getValue().getTransactionDate()).isEqualTo(LocalDateTime.now(clock));
    }

    @Test
    void findBySellerId_propagatesNotFoundFromSellerService() {
        when(sellerService.getSellerOrThrow(99L))
                .thenThrow(ResourceNotFoundException.seller(99L));

        assertThatThrownBy(() -> service.findBySellerId(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findBySellerId_returnsTransactionsEvenForSoftDeletedSeller() {
        Seller deleted = seller();
        deleted.markDeleted();

        when(sellerService.getSellerOrThrow(1L)).thenReturn(deleted);

        Transaction tx = new Transaction(deleted, new BigDecimal("100.00"),
                PaymentType.CARD, LocalDateTime.now(clock));
        when(transactionRepository.findAllBySellerIdOrderByTransactionDateAsc(1L))
                .thenReturn(List.of(tx));

        List<TransactionResponse> result = service.findBySellerId(1L);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().amount()).isEqualByComparingTo("100.00");
    }

    @Test
    void findById_missing_throwsNotFound() {
        when(transactionRepository.findById(7L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById(7L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findAll_mapsAllTransactions() {
        Seller s = seller();
        Transaction t = new Transaction(s, new BigDecimal("10.00"),
                PaymentType.TRANSFER, LocalDateTime.now(clock));
        when(transactionRepository.findAll()).thenReturn(List.of(t));

        List<TransactionResponse> result = service.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().paymentType()).isEqualTo(PaymentType.TRANSFER);
    }
}
