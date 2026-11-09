package com.walletservice.repository;

import com.walletservice.entity.ScheduleStatus;
import com.walletservice.entity.ScheduledTransfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ScheduledTransferRepository extends JpaRepository<ScheduledTransfer, Long> {

    List<ScheduledTransfer> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT st FROM ScheduledTransfer st WHERE st.status = :status AND st.nextRunAt <= :cutoff")
    List<ScheduledTransfer> findDueTransfers(@Param("status") ScheduleStatus status, @Param("cutoff") Instant cutoff);
}
