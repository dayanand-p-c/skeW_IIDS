package com.dis.repository;

import com.dis.model.RiskSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RiskSnapshotRepository extends JpaRepository<RiskSnapshot, Long> {

    @Query("SELECT r FROM RiskSnapshot r JOIN FETCH r.drug LEFT JOIN FETCH r.batch " +
           "WHERE r.snapshotDate = :date AND r.batch IS NULL")
    List<RiskSnapshot> findDrugSnapshotsByDate(@Param("date") LocalDate date);

    @Query("SELECT r FROM RiskSnapshot r JOIN FETCH r.drug LEFT JOIN FETCH r.batch " +
           "WHERE r.snapshotDate = :date")
    List<RiskSnapshot> findAllSnapshotsByDate(@Param("date") LocalDate date);

    @Query("SELECT r FROM RiskSnapshot r WHERE r.drug.drugId = :drugId AND r.batch IS NULL " +
           "AND r.snapshotDate BETWEEN :startDate AND :endDate ORDER BY r.snapshotDate ASC")
    List<RiskSnapshot> findDrugSnapshotHistory(@Param("drugId") String drugId,
                                              @Param("startDate") LocalDate startDate,
                                              @Param("endDate") LocalDate endDate);

    @Query("SELECT r FROM RiskSnapshot r WHERE r.batch IS NULL " +
           "AND r.snapshotDate BETWEEN :startDate AND :endDate ORDER BY r.snapshotDate ASC")
    List<RiskSnapshot> findAllDrugSnapshotHistory(@Param("startDate") LocalDate startDate,
                                                 @Param("endDate") LocalDate endDate);
}
