package com.shift.crm.service;

import com.shift.crm.dto.response.BestPeriodResponse;
import com.shift.crm.dto.response.ProductiveSellerResponse;
import com.shift.crm.dto.response.SellerSumResponse;
import com.shift.crm.exception.BadRequestException;
import com.shift.crm.mapper.SellerMapper;
import com.shift.crm.model.PaymentType;
import com.shift.crm.model.PeriodType;
import com.shift.crm.model.Seller;
import com.shift.crm.model.Transaction;
import com.shift.crm.repository.SellerRepository;
import com.shift.crm.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private SellerRepository sellerRepository;
    @Mock private SellerService sellerService;

    private AnalyticsService service;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(
                Instant.parse("2026-05-24T12:00:00Z"),
                ZoneId.of("UTC")
        );
        service = new AnalyticsService(
                transactionRepository,
                sellerRepository,
                sellerService,
                new SellerMapper(),
                fixedClock
        );
    }

    private static Seller sellerWithId(long id, String name) {
        Seller seller = new Seller(name, name + "@example.com",
                LocalDateTime.of(2025, 1, 1, 0, 0));
        try {
            Field f = Seller.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(seller, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        return seller;
    }

    private static Transaction tx(Seller seller, String amount, LocalDateTime date) {
        return new Transaction(seller, new BigDecimal(amount), PaymentType.CARD, date);
    }

    @Test
    void findMostProductiveSellers_returnsTopSeller() {
        when(transactionRepository.aggregateAmountBySellerBetween(any(), any()))
                .thenReturn(List.of(
                        new Object[]{1L, new BigDecimal("500.00")},
                        new Object[]{2L, new BigDecimal("1500.00")}
                ));
        when(sellerService.getActiveSellerOrThrow(2L)).thenReturn(sellerWithId(2L, "Мария"));

        List<ProductiveSellerResponse> result =
                service.findMostProductiveSellers(PeriodType.DAY);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).seller().id()).isEqualTo(2L);
        assertThat(result.get(0).totalAmount()).isEqualByComparingTo("1500.00");
    }

    @Test
    void findMostProductiveSellers_returnsAllOnTie() {
        when(transactionRepository.aggregateAmountBySellerBetween(any(), any()))
                .thenReturn(List.of(
                        new Object[]{1L, new BigDecimal("1000.00")},
                        new Object[]{2L, new BigDecimal("1000.00")},
                        new Object[]{3L, new BigDecimal("500.00")}
                ));
        when(sellerService.getActiveSellerOrThrow(1L)).thenReturn(sellerWithId(1L, "Иван"));
        when(sellerService.getActiveSellerOrThrow(2L)).thenReturn(sellerWithId(2L, "Мария"));

        List<ProductiveSellerResponse> result =
                service.findMostProductiveSellers(PeriodType.DAY);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(r -> r.seller().id())
                .containsExactly(1L, 2L);
    }

    @Test
    void findMostProductiveSellers_returnsEmptyWhenNoData() {
        when(transactionRepository.aggregateAmountBySellerBetween(any(), any()))
                .thenReturn(List.of());

        assertThat(service.findMostProductiveSellers(PeriodType.YEAR)).isEmpty();
    }


    @Test
    void findSellersWithSumLessThan_includesSellersWithoutTransactions() {
        Seller s1 = sellerWithId(1L, "Иван");
        Seller s2 = sellerWithId(2L, "Мария");
        Seller s3 = sellerWithId(3L, "Алексей");
        when(sellerRepository.findAllByDeletedFalse()).thenReturn(List.of(s1, s2, s3));
        when(transactionRepository.aggregateAmountBySellerBetween(any(), any()))
                .thenReturn(List.of(
                        new Object[]{1L, new BigDecimal("100.00")},
                        new Object[]{2L, new BigDecimal("9999.00")}
                ));

        List<SellerSumResponse> result = service.findSellersWithSumLessThan(
                LocalDateTime.of(2026, 5, 1, 0, 0),
                LocalDateTime.of(2026, 5, 31, 23, 59),
                new BigDecimal("1000.00")
        );

        assertThat(result).extracting(r -> r.seller().id())
                .containsExactlyInAnyOrder(1L, 3L);
    }

    @Test
    void findSellersWithSumLessThan_throwsOnInvalidRange() {
        assertThatThrownBy(() -> service.findSellersWithSumLessThan(
                LocalDateTime.of(2026, 5, 10, 0, 0),
                LocalDateTime.of(2026, 5, 1, 0, 0),
                BigDecimal.TEN))
                .isInstanceOf(BadRequestException.class);
    }


    @Test
    void findBestPeriod_returnsWindowWithMostTransactions() {
        Seller seller = sellerWithId(1L, "Иван");
        when(sellerService.getSellerOrThrow(1L)).thenReturn(seller);


        List<Transaction> txs = List.of(
                tx(seller, "100", LocalDateTime.of(2026, 1, 1, 10, 0)),
                tx(seller, "200", LocalDateTime.of(2026, 1, 1, 18, 0)),
                tx(seller, "300", LocalDateTime.of(2026, 1, 2, 12, 0)),
                tx(seller, "999", LocalDateTime.of(2026, 4, 1, 12, 0))
        );
        when(transactionRepository.findAllBySellerIdOrderByTransactionDateAsc(1L))
                .thenReturn(txs);

        BestPeriodResponse r = service.findBestPeriod(1L, 7L).orElseThrow();

        assertThat(r.transactionCount()).isEqualTo(3);
        assertThat(r.totalAmount()).isEqualByComparingTo("600");
        assertThat(r.periodStart()).isEqualTo(LocalDateTime.of(2026, 1, 1, 10, 0));
        assertThat(r.periodEnd()).isEqualTo(LocalDateTime.of(2026, 1, 8, 10, 0));
        assertThat(r.periodDays()).isEqualTo(7);
    }

    @Test
    void findBestPeriod_tieByCount_choosesHigherSum() {
        Seller seller = sellerWithId(1L, "Иван");
        when(sellerService.getSellerOrThrow(1L)).thenReturn(seller);


        List<Transaction> txs = List.of(
                tx(seller, "10",  LocalDateTime.of(2026, 1, 1, 10, 0)),
                tx(seller, "20",  LocalDateTime.of(2026, 1, 1, 12, 0)),
                tx(seller, "500", LocalDateTime.of(2026, 2, 1, 10, 0)),
                tx(seller, "500", LocalDateTime.of(2026, 2, 1, 12, 0))
        );
        when(transactionRepository.findAllBySellerIdOrderByTransactionDateAsc(1L))
                .thenReturn(txs);

        BestPeriodResponse r = service.findBestPeriod(1L, 1L).orElseThrow();

        assertThat(r.transactionCount()).isEqualTo(2);
        assertThat(r.totalAmount()).isEqualByComparingTo("1000");
        assertThat(r.periodStart()).isEqualTo(LocalDateTime.of(2026, 2, 1, 10, 0));
    }

    @Test
    void findBestPeriod_returnsEmptyWhenNoTransactions() {
        Seller seller = sellerWithId(1L, "Иван");
        when(sellerService.getSellerOrThrow(1L)).thenReturn(seller);
        when(transactionRepository.findAllBySellerIdOrderByTransactionDateAsc(1L))
                .thenReturn(List.of());

        assertThat(service.findBestPeriod(1L, 7L)).isEmpty();
    }

    @Test
    void findBestPeriod_throwsOnNonPositivePeriod() {
        assertThatThrownBy(() -> service.findBestPeriod(1L, 0L))
                .isInstanceOf(BadRequestException.class);
    }
}