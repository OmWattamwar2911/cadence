package com.walletservice.repository;

import com.walletservice.entity.EntryType;
import com.walletservice.entity.LedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    @Query("SELECT le FROM LedgerEntry le WHERE le.wallet.id = :walletId " +
           "AND (:entryType IS NULL OR le.entryType = :entryType) " +
           "AND (:fromTime IS NULL OR le.createdAt >= :fromTime) " +
           "AND (:toTime IS NULL OR le.createdAt <= :toTime) " +
           "ORDER BY le.createdAt DESC, le.id DESC")
    Page<LedgerEntry> findTransactions(
            @Param("walletId") Long walletId,
            @Param("entryType") EntryType entryType,
            @Param("fromTime") Instant fromTime,
            @Param("toTime") Instant toTime,
            Pageable pageable);

    @Query("SELECT COALESCE(SUM(CASE WHEN le.entryType IN ('CREDIT', 'DEPOSIT') THEN le.amount ELSE -le.amount END), 0) " +
           "FROM LedgerEntry le WHERE le.wallet.id = :walletId")
    Long calculateReconciledBalance(@Param("walletId") Long walletId);
}
