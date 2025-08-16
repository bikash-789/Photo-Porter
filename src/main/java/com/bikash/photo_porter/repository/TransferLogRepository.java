package com.bikash.photo_porter.repository;

import com.bikash.photo_porter.model.TransferLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransferLogRepository extends JpaRepository<TransferLog, Long> {
    List<TransferLog> findByTransferIdOrderByTimestampAsc(Long transferId);
}
