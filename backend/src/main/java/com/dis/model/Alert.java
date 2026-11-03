package com.dis.model;

import com.dis.model.enums.AlertSeverity;
import com.dis.model.enums.AlertStatus;
import com.dis.model.enums.AlertType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "alert", indexes = {
    @Index(name = "idx_alert_status_type", columnList = "status, type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alert {

    @Id
    @Column(name = "alert_id", length = 100, nullable = false)
    private String alertId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "drug_id", nullable = false)
    private DrugMaster drug;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private Batch batch;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private AlertType type;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private AlertSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private AlertStatus status;

    @Column(name = "risk_score", precision = 5, scale = 2, nullable = false)
    private BigDecimal riskScore;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;
}
