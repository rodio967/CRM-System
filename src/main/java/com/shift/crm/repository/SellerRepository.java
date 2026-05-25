package com.shift.crm.repository;

import com.shift.crm.model.Seller;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SellerRepository extends JpaRepository<Seller, Long> {

    List<Seller> findAllByDeletedFalse();

    Optional<Seller> findByIdAndDeletedFalse(Long id);

}
