package com.dis.service;

import com.dis.dto.stock.DrugRiskBreakdownDto;
import com.dis.dto.stock.ReorderSuggestionDto;
import com.dis.dto.stock.StockOverviewDto;
import com.dis.engine.*;
import com.dis.exception.ResourceNotFoundException;
import com.dis.model.Batch;
import com.dis.model.DrugMaster;
import com.dis.model.Supplier;
import com.dis.model.Transaction;
import com.dis.model.enums.BatchStatus;
import com.dis.repository.*;
import lombok.RequiredArgsConstructor;
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
public class StockService {

    private final DrugMasterRepository drugRepository;
    private final BatchRepository batchRepository;
    private final TransactionRepository transactionRepository;
    private final SystemConfigRepository configRepository;
    private final AlertRepository alertRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<StockOverviewDto> getStockOverview(String category) {
        List<DrugMaster> drugs = drugRepository.findAll();
        if (category != null && !category.isBlank()) {
            drugs = drugs.stream()
                    .filter(d -> d.getCategory().equalsIgnoreCase(category))
                    .collect(Collectors.toList());
        }

        List<StockOverviewDto> list = new ArrayList<>();
        for (DrugMaster drug : drugs) {
            list.add(calculateOverview(drug));
        }
        return list;
    }

    @Transactional(readOnly = true)
    public DrugRiskBreakdownDto getDrugRiskBreakdown(String drugId) {
        DrugMaster drug = drugRepository.findById(drugId)
                .orElseThrow(() -> new ResourceNotFoundException("Drug not found: " + drugId));

        LocalDate today = LocalDate.now(clock);
        Instant thirtyDaysAgo = clock.instant().minus(30, ChronoUnit.DAYS);

        double wS = Double.parseDouble(configRepository.getValueOrDefault("weight.stock", "0.4"));
        double wE = Double.parseDouble(configRepository.getValueOrDefault("weight.expiry", "0.4"));
        double wV = Double.parseDouble(configRepository.getValueOrDefault("weight.variability", "0.2"));
        double watchBand = Double.parseDouble(configRepository.getValueOrDefault("band.watch", "30"));
        double criticalBand = Double.parseDouble(configRepository.getValueOrDefault("band.critical", "60"));

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
        DemandCalculator.DemandResult demand = DemandCalculator.calculate(dailyDemandList, activeStock, 1.5);

        List<Batch> activeBatches = batchRepository.findActiveBatchesByDrug(drug.getDrugId(), BatchStatus.ACTIVE);
        Set<Integer> leadTimes = activeBatches.stream().map(b -> b.getSupplier().getLeadTimeDays()).collect(Collectors.toSet());
        int leadTimeDays = LeadTimeCalculator.calculate(leadTimes, 7);

        double serviceLevel = drug.getServiceLevel() != null ? drug.getServiceLevel().doubleValue() : 0.95;
        SafetyStockCalculator.StockParameters stockParams = SafetyStockCalculator.calculate(
                demand.getMa30(), demand.getSigma(), leadTimeDays, serviceLevel);

        List<Long> batchDtes = new ArrayList<>();
        List<String> batchIds = new ArrayList<>();
        for (Batch b : activeBatches) {
            batchDtes.add(ChronoUnit.DAYS.between(today, b.getExpiryDate()));
            batchIds.add(b.getBatchId());
        }

        SafetyScoreCalculator.DrugScore score = SafetyScoreCalculator.calculateDrugScore(
                activeStock, stockParams.getSafetyStock(), stockParams.getReorderPoint(),
                demand.getSigma(), demand.getMa30(), batchDtes, batchIds,
                wS, wE, wV, watchBand, criticalBand
        );

        return DrugRiskBreakdownDto.builder()
                .drugId(drug.getDrugId())
                .drugName(drug.getName())
                .stockRisk(score.getStockRisk())
                .expiryRisk(score.getExpiryRisk())
                .demandVariability(score.getVariabilityRisk())
                .riskIndex(score.getRiskIndex())
                .status(score.getStatus())
                .currentStock(activeStock)
                .safetyStock(stockParams.getSafetyStock())
                .reorderPoint(stockParams.getReorderPoint())
                .ma30(demand.getMa30())
                .ma7(demand.getMa7())
                .sigma(demand.getSigma())
                .batchScores(score.getBatchScores())
                .build();
    }

    @Transactional(readOnly = true)
    public List<ReorderSuggestionDto> getReorderSuggestions() {
        List<DrugMaster> drugs = drugRepository.findAll();
        LocalDate today = LocalDate.now(clock);
        Instant thirtyDaysAgo = clock.instant().minus(30, ChronoUnit.DAYS);

        List<ReorderSuggestionDto> suggestions = new ArrayList<>();

        for (DrugMaster drug : drugs) {
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
            DemandCalculator.DemandResult demand = DemandCalculator.calculate(dailyDemandList, activeStock, 1.5);

            List<Batch> activeBatches = batchRepository.findActiveBatchesByDrug(drug.getDrugId(), BatchStatus.ACTIVE);
            Set<Integer> leadTimes = activeBatches.stream().map(b -> b.getSupplier().getLeadTimeDays()).collect(Collectors.toSet());
            int leadTimeDays = LeadTimeCalculator.calculate(leadTimes, 7);

            double serviceLevel = drug.getServiceLevel() != null ? drug.getServiceLevel().doubleValue() : 0.95;
            SafetyStockCalculator.StockParameters stockParams = SafetyStockCalculator.calculate(
                    demand.getMa30(), demand.getSigma(), leadTimeDays, serviceLevel);

            // Compute average shelf life of batches
            double avgShelfLifeDays = 365.0;
            if (!activeBatches.isEmpty()) {
                double totalShelfDays = activeBatches.stream()
                        .mapToDouble(b -> ChronoUnit.DAYS.between(b.getManufacturingDate(), b.getExpiryDate()))
                        .sum();
                avgShelfLifeDays = totalShelfDays / activeBatches.size();
            }

            boolean hasNearExpiryAlert = !alertRepository.findOpenNearExpiryAlertsByDrug(drug.getDrugId()).isEmpty();

            ReorderSuggestionCalculator.ReorderSuggestion res = ReorderSuggestionCalculator.calculate(
                    activeStock,
                    stockParams.getReorderPoint(),
                    stockParams.getSafetyStock(),
                    demand.getMa30(),
                    leadTimeDays,
                    avgShelfLifeDays,
                    hasNearExpiryAlert
            );

            if (res.isSuggested() || res.isSuppressedByNearExpiry()) {
                Supplier primarySupplier = activeBatches.isEmpty() ? null : activeBatches.get(0).getSupplier();
                BigDecimal cost = drug.getUnitCost().multiply(BigDecimal.valueOf(res.getSuggestedQuantity()));

                String urgency = "MEDIUM";
                if (activeStock == 0) {
                    urgency = "CRITICAL";
                } else if (activeStock <= stockParams.getSafetyStock()) {
                    urgency = "HIGH";
                }

                suggestions.add(ReorderSuggestionDto.builder()
                        .drugId(drug.getDrugId())
                        .drugName(drug.getName())
                        .category(drug.getCategory())
                        .primarySupplierId(primarySupplier != null ? primarySupplier.getSupplierId() : null)
                        .primarySupplierName(primarySupplier != null ? primarySupplier.getName() : null)
                        .leadTimeDays(leadTimeDays)
                        .currentStock(activeStock)
                        .reorderPoint(stockParams.getReorderPoint())
                        .safetyStock(stockParams.getSafetyStock())
                        .suggestedQuantity(res.getSuggestedQuantity())
                        .estimatedCost(cost)
                        .ma30(demand.getMa30())
                        .daysOfCover(demand.getDaysOfCover())
                        .urgency(urgency)
                        .suppressedByNearExpiry(res.isSuppressedByNearExpiry())
                        .reason(res.getReason())
                        .build());
            }
        }

        return suggestions;
    }

    private StockOverviewDto calculateOverview(DrugMaster drug) {
        LocalDate today = LocalDate.now(clock);
        Instant thirtyDaysAgo = clock.instant().minus(30, ChronoUnit.DAYS);

        double wS = Double.parseDouble(configRepository.getValueOrDefault("weight.stock", "0.4"));
        double wE = Double.parseDouble(configRepository.getValueOrDefault("weight.expiry", "0.4"));
        double wV = Double.parseDouble(configRepository.getValueOrDefault("weight.variability", "0.2"));
        double watchBand = Double.parseDouble(configRepository.getValueOrDefault("band.watch", "30"));
        double criticalBand = Double.parseDouble(configRepository.getValueOrDefault("band.critical", "60"));

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
        DemandCalculator.DemandResult demand = DemandCalculator.calculate(dailyDemandList, activeStock, 1.5);

        List<Batch> activeBatches = batchRepository.findActiveBatchesByDrug(drug.getDrugId(), BatchStatus.ACTIVE);
        Set<Integer> leadTimes = activeBatches.stream().map(b -> b.getSupplier().getLeadTimeDays()).collect(Collectors.toSet());
        int leadTimeDays = LeadTimeCalculator.calculate(leadTimes, 7);

        double serviceLevel = drug.getServiceLevel() != null ? drug.getServiceLevel().doubleValue() : 0.95;
        SafetyStockCalculator.StockParameters stockParams = SafetyStockCalculator.calculate(
                demand.getMa30(), demand.getSigma(), leadTimeDays, serviceLevel);

        List<Long> batchDtes = new ArrayList<>();
        List<String> batchIds = new ArrayList<>();
        for (Batch b : activeBatches) {
            batchDtes.add(ChronoUnit.DAYS.between(today, b.getExpiryDate()));
            batchIds.add(b.getBatchId());
        }

        SafetyScoreCalculator.DrugScore score = SafetyScoreCalculator.calculateDrugScore(
                activeStock, stockParams.getSafetyStock(), stockParams.getReorderPoint(),
                demand.getSigma(), demand.getMa30(), batchDtes, batchIds,
                wS, wE, wV, watchBand, criticalBand
        );

        return StockOverviewDto.builder()
                .drugId(drug.getDrugId())
                .drugName(drug.getName())
                .category(drug.getCategory())
                .unit(drug.getUnit())
                .unitCost(drug.getUnitCost())
                .currentStock(activeStock)
                .safetyStock(stockParams.getSafetyStock())
                .reorderPoint(stockParams.getReorderPoint())
                .ma7(demand.getMa7())
                .ma30(demand.getMa30())
                .daysOfCover(demand.getDaysOfCover())
                .spike(demand.isSpike())
                .riskIndex(score.getRiskIndex())
                .status(score.getStatus())
                .build();
    }
}
