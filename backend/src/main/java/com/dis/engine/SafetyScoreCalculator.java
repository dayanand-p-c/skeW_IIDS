package com.dis.engine;

import com.dis.model.enums.RiskStatus;
import lombok.Builder;
import lombok.Data;

import java.util.List;

public class SafetyScoreCalculator {

    @Data
    @Builder
    public static class BatchScore {
        private String batchId;
        private double stockRisk;      // S
        private double expiryRisk;     // E
        private double variabilityRisk; // V
        private double riskIndex;      // 0 to 100
        private RiskStatus status;
    }

    @Data
    @Builder
    public static class DrugScore {
        private double stockRisk;
        private double expiryRisk;
        private double variabilityRisk;
        private double riskIndex;
        private RiskStatus status;
        private List<BatchScore> batchScores;
    }

    /**
     * Stock risk S: 1 if stock <= SS; 0 if stock >= 2*ROP; linear in between.
     */
    public static double calculateStockRisk(long stock, int ss, int rop) {
        if (stock <= ss) {
            return 1.0;
        }
        double upperThreshold = 2.0 * rop;
        if (stock >= upperThreshold || upperThreshold <= ss) {
            return 0.0;
        }
        double linear = (upperThreshold - stock) / (upperThreshold - ss);
        return Math.max(0.0, Math.min(1.0, linear));
    }

    /**
     * Expiry risk E: 1 - min(dte,180)/180; 1 if expired.
     */
    public static double calculateExpiryRisk(long dte) {
        if (dte <= 0) {
            return 1.0;
        }
        return 1.0 - (Math.min((double) dte, 180.0) / 180.0);
    }

    /**
     * Demand variability V: min(sigma/mean, 1).
     */
    public static double calculateVariabilityRisk(double sigma, double mean) {
        if (mean <= 0.00001) {
            return 0.0;
        }
        return Math.min(sigma / mean, 1.0);
    }

    /**
     * Resolves status based on watchBand (default 30) and criticalBand (default 60).
     * SAFE 0-30, WATCH 31-60, CRITICAL 61-100.
     */
    public static RiskStatus resolveStatus(double riskIndex, double watchBand, double criticalBand) {
        if (riskIndex <= watchBand) {
            return RiskStatus.SAFE;
        } else if (riskIndex <= criticalBand) {
            return RiskStatus.WATCH;
        } else {
            return RiskStatus.CRITICAL;
        }
    }

    /**
     * Calculates index = 100 * (wS*S + wE*E + wV*V).
     */
    public static double calculateIndex(double s, double e, double v, double wS, double wE, double wV) {
        double raw = (wS * s) + (wE * e) + (wV * v);
        double index = raw * 100.0;
        return Math.max(0.0, Math.min(100.0, Math.round(index * 100.0) / 100.0));
    }

    /**
     * Calculates composite score for a drug and all its batches.
     */
    public static DrugScore calculateDrugScore(
            long currentStock,
            int ss,
            int rop,
            double sigma,
            double mean,
            List<Long> batchDtes,
            List<String> batchIds,
            double wS,
            double wE,
            double wV,
            double watchBand,
            double criticalBand) {

        double s = calculateStockRisk(currentStock, ss, rop);
        double v = calculateVariabilityRisk(sigma, mean);

        if (batchDtes == null || batchDtes.isEmpty()) {
            double drugIndex = calculateIndex(s, 0.0, v, wS, 0.0, wV);
            // If no batches, normalize weights across wS and wV
            double sumWeights = wS + wV;
            if (sumWeights > 0) {
                drugIndex = calculateIndex(s, 0.0, v, wS / sumWeights, 0.0, wV / sumWeights);
            }
            RiskStatus status = resolveStatus(drugIndex, watchBand, criticalBand);

            return DrugScore.builder()
                    .stockRisk(s)
                    .expiryRisk(0.0)
                    .variabilityRisk(v)
                    .riskIndex(drugIndex)
                    .status(status)
                    .batchScores(List.of())
                    .build();
        }

        List<BatchScore> batchScores = new java.util.ArrayList<>();
        double maxBatchIndex = 0.0;
        double maxBatchE = 0.0;

        for (int i = 0; i < batchDtes.size(); i++) {
            long dte = batchDtes.get(i);
            String bId = (batchIds != null && i < batchIds.size()) ? batchIds.get(i) : ("batch-" + i);
            double e = calculateExpiryRisk(dte);
            double bIndex = calculateIndex(s, e, v, wS, wE, wV);
            RiskStatus bStatus = resolveStatus(bIndex, watchBand, criticalBand);

            batchScores.add(BatchScore.builder()
                    .batchId(bId)
                    .stockRisk(s)
                    .expiryRisk(e)
                    .variabilityRisk(v)
                    .riskIndex(bIndex)
                    .status(bStatus)
                    .build());

            if (bIndex > maxBatchIndex) {
                maxBatchIndex = bIndex;
                maxBatchE = e;
            }
        }

        RiskStatus drugStatus = resolveStatus(maxBatchIndex, watchBand, criticalBand);

        return DrugScore.builder()
                .stockRisk(s)
                .expiryRisk(maxBatchE)
                .variabilityRisk(v)
                .riskIndex(maxBatchIndex)
                .status(drugStatus)
                .batchScores(batchScores)
                .build();
    }
}
