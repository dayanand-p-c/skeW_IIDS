package com.dis.service;

import com.dis.engine.*;
import com.dis.event.DrugStockChangedEvent;
import com.dis.model.*;
import com.dis.model.enums.AlertSeverity;
import com.dis.model.enums.AlertType;
import com.dis.model.enums.BatchStatus;
import com.dis.model.enums.RiskStatus;
import com.dis.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RiskEvaluationService {

    private final DrugMasterRepository drugRepository;
    private final BatchRepository batchRepository;
    private final TransactionRepository transactionRepository;
    private final RiskSnapshotRepository riskSnapshotRepository;
    private final SystemConfigRepository configRepository;
    private final AlertService alertService;
    private final Clock clock;

    @EventListener
    @Transactional
    public void onStockChanged(DrugStockChangedEvent event) {
        drugRepository.findById(event.getDrugId()).ifPresent(this::evaluateDrugRisk);
    }

    @Transactional
    public void evaluateDrugRisk(DrugMaster drug) {
        LocalDate today = LocalDate.now(clock);
        Instant thirtyDaysAgo = clock.instant().minus(30, ChronoUnit.DAYS);

        // 1. Fetch parameters from system config
        double wS = Double.parseDouble(configRepository.getValueOrDefault("weight.stock", "0.4"));
        double wE = Double.parseDouble(configRepository.getValueOrDefault("weight.expiry", "0.4"));
        double wV = Double.parseDouble(configRepository.getValueOrDefault("weight.variability", "0.2"));
        int warnDays = Integer.parseInt(configRepository.getValueOrDefault("expiry.warn.days", "90"));
        int criticalDays = Integer.parseInt(configRepository.getValueOrDefault("expiry.critical.days", "30"));
        double spikeRatio = Double.parseDouble(configRepository.getValueOrDefault("spike.ratio", "1.5"));
        double watchBand = Double.parseDouble(configRepository.getValueOrDefault("band.watch", "30"));
        double criticalBand = Double.parseDouble(configRepository.getValueOrDefault("band.critical", "60"));

        // 2. Compute Demand
        List<Transaction> outbounds = transactionRepository.findOutboundSince(drug.getDrugId(), thirtyDaysAgo);
        Map<LocalDate, Integer> dailyDemandMap = new HashMap<>();
        for (Transaction tx : outbounds) {
            LocalDate txDate = tx.getTimestamp().atZone(clock.getZone()).toLocalDate();
            dailyDemandMap.merge(txDate, tx.getQuantity(), Integer::sum);
        }

        List<Integer> dailyDemandList = new ArrayList<>();
        for (int i = 29; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            dailyDemandList.add(dailyDemandMap.getOrDefault(d, 0));
        }

        long activeStock = batchRepository.getTotalUnexpiredStockByDrug(drug.getDrugId(), today);
        DemandCalculator.DemandResult demand = DemandCalculator.calculate(dailyDemandList, activeStock, spikeRatio);

        // 3. Compute Lead Time L
        List<Batch> activeBatches = batchRepository.findActiveBatchesByDrug(drug.getDrugId(), BatchStatus.ACTIVE);
        Set<Integer> leadTimes = activeBatches.stream()
                .map(b -> b.getSupplier().getLeadTimeDays())
                .collect(Collectors.toSet());
        int leadTimeDays = LeadTimeCalculator.calculate(leadTimes, 7);

        // 4. Compute Safety Stock & ROP
        double serviceLevel = drug.getServiceLevel() != null ? drug.getServiceLevel().doubleValue() : 0.95;
        SafetyStockCalculator.StockParameters stockParams = SafetyStockCalculator.calculate(
                demand.getMa30(),
                demand.getSigma(),
                leadTimeDays,
                serviceLevel
        );

        // 5. Evaluate Batches and Safety Score
        List<Long> batchDtes = new ArrayList<>();
        List<String> batchIds = new ArrayList<>();

        for (Batch batch : activeBatches) {
            long dte = ChronoUnit.DAYS.between(today, batch.getExpiryDate());
            batchDtes.add(dte);
            batchIds.add(batch.getBatchId());

            ExpiryCalculator.ExpiryStatus expStatus = ExpiryCalculator.evaluate(
                    today, batch.getExpiryDate(), batch.getQuantity(), demand.getMa7(), warnDays, criticalDays
            );

            // Batch-level NEAR_EXPIRY alert
            if (expStatus.isNearExpiry() && expStatus.getSeverity() != null && batch.getQuantity() > 0) {
                alertService.raiseOrEscalateBatchAlert(drug, batch, AlertType.NEAR_EXPIRY, expStatus.getSeverity(), 80.0);
            } else if (batch.getQuantity() == 0 || expStatus.isExpired()) {
                alertService.autoResolveBatchAlert(batch.getBatchId(), AlertType.NEAR_EXPIRY);
            }
        }

        SafetyScoreCalculator.DrugScore drugScore = SafetyScoreCalculator.calculateDrugScore(
                activeStock,
                stockParams.getSafetyStock(),
                stockParams.getReorderPoint(),
                demand.getSigma(),
                demand.getMa30(),
                batchDtes,
                batchIds,
                wS, wE, wV,
                watchBand, criticalBand
        );

        // 6. Drug-level LOW_STOCK Alert
        if (activeStock <= stockParams.getSafetyStock() && stockParams.getSafetyStock() > 0) {
            alertService.raiseOrEscalateDrugAlert(drug, AlertType.LOW_STOCK, AlertSeverity.CRITICAL, drugScore.getRiskIndex());
        } else if (activeStock <= stockParams.getReorderPoint() && stockParams.getReorderPoint() > 0) {
            alertService.raiseOrEscalateDrugAlert(drug, AlertType.LOW_STOCK, AlertSeverity.WARNING, drugScore.getRiskIndex());
        } else {
            alertService.autoResolveDrugAlert(drug.getDrugId(), AlertType.LOW_STOCK);
        }

        // 7. Drug-level HIGH_RISK Alert (when risk index is in CRITICAL band)
        if (drugScore.getStatus() == RiskStatus.CRITICAL) {
            alertService.raiseOrEscalateDrugAlert(drug, AlertType.HIGH_RISK, AlertSeverity.CRITICAL, drugScore.getRiskIndex());
        } else {
            alertService.autoResolveDrugAlert(drug.getDrugId(), AlertType.HIGH_RISK);
        }

        // 8. Snapshot recording
        recordSnapshot(drug, null, drugScore.getStockRisk(), drugScore.getExpiryRisk(),
                drugScore.getVariabilityRisk(), drugScore.getRiskIndex(), drugScore.getStatus(),
                stockParams.getReorderPoint(), stockParams.getSafetyStock(), today);
    }

    private void recordSnapshot(DrugMaster drug, Batch batch, double s, double e, double v,
                                double index, RiskStatus status, int rop, int ss, LocalDate today) {
        RiskSnapshot snapshot = RiskSnapshot.builder()
                .snapshotDate(today)
                .drug(drug)
                .batch(batch)
                .stockRisk(BigDecimal.valueOf(s))
                .expiryRisk(BigDecimal.valueOf(e))
                .demandVariability(BigDecimal.valueOf(v))
                .riskIndex(BigDecimal.valueOf(index))
                .status(status)
                .reorderPoint(rop)
                .safetyStock(ss)
                .build();
        riskSnapshotRepository.save(snapshot);
    }

    @Transactional
    public void evaluateAllDrugs() {
        List<DrugMaster> drugs = drugRepository.findAll();
        for (DrugMaster drug : drugs) {
            evaluateDrugRisk(drug);
        }
    }
}
