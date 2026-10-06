package com.smartmenu.repository;

import com.smartmenu.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByTransactionCode(String transactionCode);

    List<Payment> findByUserIdOrderByCreatedDateDesc(Long userId);

    Optional<Payment> findFirstByUserIdAndStatusAndConsumedFalseOrderByCreatedDateAsc(Long userId, String status);

    boolean existsByUserIdAndStatusAndConsumedFalse(Long userId, String status);
}
