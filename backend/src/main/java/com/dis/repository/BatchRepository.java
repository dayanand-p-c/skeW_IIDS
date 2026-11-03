package com.dis.repository;

import com.dis.model.Batch;
import com.dis.model.enums.BatchStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BatchRepository extends JpaRepository<Batch, String> {

    @Query("SELECT b FROM Batch b JOIN FETCH b.drug JOIN FETCH b.supplier WHERE b.batchId = :batchId")
    Optional<Batch> findByIdWithDetails(@Param("batchId") String batchId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Batch b WHERE b.batchId = :batchId")
    Optional<Batch> findByIdForUpdate(@Param("batchId") String batchId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Batch b WHERE b.drug.drugId = :drugId " +
           "AND b.status = :status AND b.expiryDate > :today AND b.quantity > 0 " +
           "ORDER BY b.expiryDate ASC, b.manufacturingDate ASC")
    List<Batch> findAvailableBatchesForDispenseWithLock(@Param("drugId") String drugId,
                                                        @Param("status") BatchStatus status,
                                                        @Param("today") LocalDate today);

    @Query("SELECT b FROM Batch b WHERE b.drug.drugId = :drugId AND b.status = :status AND b.quantity > 0 ORDER BY b.expiryDate ASC")
    List<Batch> findActiveBatchesByDrug(@Param("drugId") String drugId, @Param("status") BatchStatus status);

    @Query("SELECT b FROM Batch b WHERE b.status = :status AND b.quantity > 0")
    List<Batch> findAllActiveBatchesWithStock(@Param("status") BatchStatus status);

    @Query("SELECT b FROM Batch b WHERE " +
           "(:drugId IS NULL OR b.drug.drugId = :drugId) AND " +
           "(:status IS NULL OR b.status = :status) AND " +
           "(:query IS NULL OR LOWER(b.batchId) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(b.drug.name) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Batch> searchBatches(@Param("drugId") String drugId,
                              @Param("status") BatchStatus status,
                              @Param("query") String query,
                              Pageable pageable);

    @Query("SELECT COALESCE(SUM(b.quantity), 0) FROM Batch b WHERE b.drug.drugId = :drugId AND b.status = 'ACTIVE' AND b.expiryDate > :today")
    long getTotalUnexpiredStockByDrug(@Param("drugId") String drugId, @Param("today") LocalDate today);

    @Query("SELECT COALESCE(SUM(b.quantity), 0) FROM Batch b WHERE b.drug.drugId = :drugId AND b.status = 'ACTIVE'")
    long getTotalActiveStockByDrug(@Param("drugId") String drugId);

    @Query("SELECT b FROM Batch b WHERE b.status = 'ACTIVE' AND b.expiryDate <= :today AND b.quantity > 0")
    List<Batch> findExpiredBatchesWithStock(@Param("today") LocalDate today);
}
