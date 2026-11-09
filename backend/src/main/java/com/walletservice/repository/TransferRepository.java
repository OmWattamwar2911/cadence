package com.walletservice.repository;

import com.walletservice.entity.Transfer;
import com.walletservice.entity.TransferStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface TransferRepository extends JpaRepository<Transfer, Long> {

    @Query("SELECT COUNT(t) FROM Transfer t WHERE t.fromWallet.id = :walletId AND t.createdAt >= :since")
    long countTransfersSince(@Param("walletId") Long walletId, @Param("since") Instant since);

    @Query("SELECT COUNT(t) FROM Transfer t WHERE t.fromWallet.id = :walletId AND t.status = :status AND t.createdAt >= :since")
    long countTransfersByStatusSince(
            @Param("walletId") Long walletId,
            @Param("status") TransferStatus status,
            @Param("since") Instant since);

    List<Transfer> findByFromWalletIdAndStatus(Long fromWalletId, TransferStatus status);
}
