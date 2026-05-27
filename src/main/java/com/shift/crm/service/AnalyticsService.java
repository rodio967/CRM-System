package com.shift.crm.service;


import com.shift.crm.dto.response.BestPeriodResponse;
import com.shift.crm.dto.response.ProductiveSellerResponse;
import com.shift.crm.dto.response.SellerSumResponse;
import com.shift.crm.exception.BadRequestException;
import com.shift.crm.mapper.SellerMapper;
import com.shift.crm.model.PeriodType;
import com.shift.crm.model.Seller;
import com.shift.crm.model.Transaction;
import com.shift.crm.repository.SellerRepository;
import com.shift.crm.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AnalyticsService {

    private final TransactionRepository transactionRepository;
    private final SellerRepository sellerRepository;
    private final SellerService sellerService;
    private final SellerMapper sellerMapper;
    private final Clock clock;


    // 1 запрос
    public List<ProductiveSellerResponse> findMostProductiveSellers(PeriodType periodType) {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime start = periodType.startOf(now);

        return findMostProductiveSellerBetween(periodType, start, now);
    }

    private List<ProductiveSellerResponse> findMostProductiveSellerBetween(PeriodType periodType, LocalDateTime from, LocalDateTime to) {
        validateRange(from, to);

        Map<Long, BigDecimal> sumsBySeller = aggregateSums(from, to);
        if (sumsBySeller.isEmpty()) {
            return List.of();
        }

        List<Long> sellerIds = findSellerIds(sumsBySeller);
        Collections.sort(sellerIds);

        List<ProductiveSellerResponse> result = sellerIds.stream()
                .map(sellerId -> {
                    BigDecimal sum = sumsBySeller.get(sellerId);
                    Seller seller = sellerService.getActiveSellerOrThrow(sellerId);

                    return new ProductiveSellerResponse(
                            sellerMapper.toResponse(seller),
                            sum,
                            periodType,
                            from,
                            to
                    );
                })
                .toList();

        return result;
    }


    // 2 запрос
    public List<SellerSumResponse> findSellersWithSumLessThan(LocalDateTime from, LocalDateTime to, BigDecimal threshold) {
        validateRange(from, to);

        if (threshold == null) throw new BadRequestException("threshold не может быть null");

        Map<Long, BigDecimal> sumsBySeller = aggregateSums(from, to);

        return sellerRepository.findAllByDeletedFalse().stream()
                .map(seller -> {
                    BigDecimal sum = sumsBySeller.getOrDefault(seller.getId(), BigDecimal.ZERO);
                    return new SellerSumResponse(sellerMapper.toResponse(seller), sum);
                })
                .filter(resp -> resp.totalAmount().compareTo(threshold) < 0)
                .toList();
    }


    // 3 запрос
    public Optional<BestPeriodResponse> findBestPeriod(Long sellerId, Long periodDays) {
        if (periodDays <= 0) throw new BadRequestException("periodDays должен быть положительным");

        sellerService.getSellerOrThrow(sellerId);

        List<Transaction> transactions = transactionRepository.findAllBySellerIdOrderByTransactionDateAsc(sellerId);
        if (transactions.isEmpty()) return Optional.empty();

        Duration periodDuration = Duration.ofDays(periodDays);

        int bestStart = 0;
        int bestEnd = 0;
        int bestCount = 1;
        BigDecimal bestSum = transactions.getFirst().getAmount();;

        // sliding window
        int left = 0;
        BigDecimal currentSum = BigDecimal.ZERO;
        for (int right = 0; right < transactions.size(); right++) {
            currentSum = currentSum.add(transactions.get(right).getAmount());

            while(Duration.between(transactions.get(left).getTransactionDate(),
                    transactions.get(right).getTransactionDate()
            ).compareTo(periodDuration) > 0) {
                currentSum = currentSum.subtract(transactions.get(left).getAmount());
                left++;
            }

            int count = right - left + 1;

            if (count > bestCount || (count == bestCount && currentSum.compareTo(bestSum) > 0)) {
                bestCount = count;
                bestSum = currentSum;
                bestStart = left;
                bestEnd = right;
            }
        }

        LocalDateTime periodStart = transactions.get(bestStart).getTransactionDate();
        LocalDateTime periodEnd = periodStart.plus(periodDuration);

        return Optional.of(new BestPeriodResponse(
                sellerId,
                periodDays,
                periodStart,
                periodEnd,
                bestCount,
                bestSum
        ));

    }



    private List<ProductiveSellerResponse> buildSellerResponses(PeriodType periodType, LocalDateTime from, LocalDateTime to,
                                                                List<Long> sellerIds, Map<Long, BigDecimal> sumsBySeller) {
        List<ProductiveSellerResponse> result = new ArrayList<>();
        for (Long sellerId : sellerIds) {
            BigDecimal sum = sumsBySeller.get(sellerId);
            Seller seller = sellerService.getActiveSellerOrThrow(sellerId);

            ProductiveSellerResponse response = new ProductiveSellerResponse(
                    sellerMapper.toResponse(seller),
                    sum,
                    periodType,
                    from,
                    to
            );

            result.add(response);
        }
        return result;
    }

    private List<Long> findSellerIds(Map<Long, BigDecimal> sumsBySeller) {
        BigDecimal maxSum = null;
        for (BigDecimal sum : sumsBySeller.values()) {
            if (maxSum == null || sum.compareTo(maxSum) > 0) {
                maxSum = sum;
            }
        }

        List<Long> sellerIds = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> entry : sumsBySeller.entrySet()) {
            if (entry.getValue().compareTo(maxSum) == 0) {
                sellerIds.add(entry.getKey());
            }
        }

        return sellerIds;
    }

    private void validateRange(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null) {
            throw new BadRequestException("Необходимо указать параметры from и to");
        }

        if (!from.isBefore(to)) {
            throw new BadRequestException("Параметр from должен быть раньше to");
        }
    }

    private Map<Long, BigDecimal> aggregateSums(LocalDateTime from, LocalDateTime to) {
        List<Object[]> rows = transactionRepository.aggregateAmountBySellerBetween(from, to);

        Map<Long, BigDecimal> result = new HashMap<>();
        for (Object[] row : rows) {
            Long sellerId = (Long) row[0];
            BigDecimal sum = (BigDecimal) row[1];

            result.put(sellerId, sum);
        }

        return result;
    }


}
