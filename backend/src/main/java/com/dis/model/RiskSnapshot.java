package com.dis.model;

import com.dis.model.enums.RiskStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "risk_snapshot", indexes = {
    @Index(name = "idx_risk_snapshot_drug_date", columnList = "drug_id, snapshot_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "snapshot_id")
    private Long snapshotId;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "drug_id", nullable = false)
    private DrugMaster drug;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private Batch batch;

    @Column(name = "stock_risk", precision = 5, scale = 4, nullable = false)
    private BigDecimal stockRisk;

    @Column(name = "expiry_risk", precision = 5, scale = 4, nullable = false)
    private BigDecimal expiryRisk;

    @Column(name = "demand_variability", precision = 5, scale = 4, nullable = false)
    private BigDecimal demandVariability;

    @Column(name = "risk_index", precision = 5, scale = 2, nullable = false)
    private BigDecimal riskIndex;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private RiskStatus status;

    @Column(name = "reorder_point", nullable = false)
    private Integer reorderPoint;

    @Column(name = "safety_stock", nullable = false)
    private Integer safetyStock;
}
