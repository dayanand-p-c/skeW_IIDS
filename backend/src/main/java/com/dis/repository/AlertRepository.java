package com.dis.repository;

import com.dis.model.Alert;
import com.dis.model.enums.AlertSeverity;
import com.dis.model.enums.AlertStatus;
import com.dis.model.enums.AlertType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlertRepository extends JpaRepository<Alert, String> {

    @Query("SELECT a FROM Alert a JOIN FETCH a.drug LEFT JOIN FETCH a.batch " +
           "WHERE (:status IS NULL OR a.status = :status) " +
           "AND (:type IS NULL OR a.type = :type) " +
           "AND (:severity IS NULL OR a.severity = :severity)")
    Page<Alert> findAlerts(@Param("status") AlertStatus status,
                           @Param("type") AlertType type,
                           @Param("severity") AlertSeverity severity,
                           Pageable pageable);

    @Query("SELECT a FROM Alert a WHERE a.drug.drugId = :drugId AND a.batch IS NULL AND a.type = :type AND a.status IN ('OPEN', 'ACK')")
    Optional<Alert> findOpenDrugAlert(@Param("drugId") String drugId, @Param("type") AlertType type);

    @Query("SELECT a FROM Alert a WHERE a.batch.batchId = :batchId AND a.type = :type AND a.status IN ('OPEN', 'ACK')")
    Optional<Alert> findOpenBatchAlert(@Param("batchId") String batchId, @Param("type") AlertType type);

    @Query("SELECT a FROM Alert a WHERE a.status IN ('OPEN', 'ACK')")
    List<Alert> findAllActiveAlerts();

    @Query("SELECT COUNT(a) FROM Alert a WHERE a.status = 'OPEN'")
    long countOpenAlerts();

    @Query("SELECT a FROM Alert a WHERE a.drug.drugId = :drugId AND a.type = 'NEAR_EXPIRY' AND a.status IN ('OPEN', 'ACK')")
    List<Alert> findOpenNearExpiryAlertsByDrug(@Param("drugId") String drugId);
}
