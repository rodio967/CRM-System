package com.shift.crm.repository;


import com.shift.crm.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findAllBySellerIdOrderByTransactionDateAsc(Long sellerId);

    @Query("""
            select t.seller.id, sum(t.amount)
            from Transaction t
            where t.seller.deleted = false
            and t.transactionDate >= :from
            and t.transactionDate <= :to
            group by t.seller.id
            """)
    List<Object[]> aggregateAmountBySellerBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
