package com.walletservice.repository;

import com.walletservice.entity.PaymentRequest;
import com.walletservice.entity.PaymentRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRequestRepository extends JpaRepository<PaymentRequest, Long> {

    List<PaymentRequest> findByPayerEmailIgnoreCaseAndStatusOrderByCreatedAtDesc(String payerEmail, PaymentRequestStatus status);

    List<PaymentRequest> findByRequesterIdOrderByCreatedAtDesc(Long requesterId);
}
