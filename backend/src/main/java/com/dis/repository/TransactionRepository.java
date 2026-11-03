package com.dis.repository;

import com.dis.model.Transaction;
import com.dis.model.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Query("SELECT t FROM Transaction t JOIN FETCH t.batch b JOIN FETCH b.drug JOIN FETCH b.supplier " +
           "WHERE (:batchId IS NULL OR t.batch.batchId = :batchId) " +
           "AND (:drugId IS NULL OR t.batch.drug.drugId = :drugId) " +
           "AND (:type IS NULL OR t.type = :type) " +
           "AND (:fromTimestamp IS NULL OR t.timestamp >= :fromTimestamp) " +
           "AND (:toTimestamp IS NULL OR t.timestamp <= :toTimestamp) " +
           "AND (:query IS NULL OR LOWER(t.batch.drug.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(t.batch.batchId) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Transaction> searchTransactions(@Param("batchId") String batchId,
                                         @Param("drugId") String drugId,
                                         @Param("type") TransactionType type,
                                         @Param("fromTimestamp") Instant fromTimestamp,
                                         @Param("toTimestamp") Instant toTimestamp,
                                         @Param("query") String query,
                                         Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.batch.drug.drugId = :drugId " +
           "AND t.type = 'OUTBOUND' AND t.timestamp >= :since ORDER BY t.timestamp ASC")
    List<Transaction> findOutboundSince(@Param("drugId") String drugId, @Param("since") Instant since);

    @Query("SELECT t FROM Transaction t WHERE t.batch.drug.drugId = :drugId ORDER BY t.timestamp ASC")
    List<Transaction> findAllByDrugOrderByTimestampAsc(@Param("drugId") String drugId);

    @Query("SELECT t FROM Transaction t WHERE t.timestamp >= :since ORDER BY t.timestamp ASC")
    List<Transaction> findAllSince(@Param("since") Instant since);

    long countByBatch_BatchId(String batchId);
}
